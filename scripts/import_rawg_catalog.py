#!/usr/bin/env python3
"""
RAWG 游戏目录批量导入 → MySQL game 表

说明：官方 rawgpy(2019) 已不适用（无 API Key、域名 rawg.io/api 已变 api.rawg.io/api）。
本脚本沿用 rawgpy 的分页思路，使用当前 RAWG REST API + Key。

依赖: pip install pymysql requests pyyaml

用法:
  python scripts/import_rawg_catalog.py --max-pages 20000   # 用尽本月约 2 万次 API（热门排序，断点续传）
  python scripts/import_rawg_catalog.py --target 10000      # 仅导入 1 万条
  python scripts/dedup_douban_games.py                      # 豆瓣与 RAWG 去重
"""
from __future__ import annotations

import argparse
import re
import sys
import time
from pathlib import Path

import pymysql
import requests
import yaml

ROOT = Path(__file__).resolve().parent.parent
LOCAL_YML = ROOT / "backend" / "src" / "main" / "resources" / "application-local.yml"
APPLICATION_YML = ROOT / "backend" / "src" / "main" / "resources" / "application.yml"

JOB_NAME = "rawg_popular_import"
RAWG_BASE = "https://api.rawg.io/api"
USER_AGENT = "GRecord/1.0 (catalog-import; +https://rawg.io/apidocs)"
PAGE_SIZE = 40
ORDERING = "-rating"
DEFAULT_TARGET = 10_000
MONTHLY_MAX_PAGES = 20_000  # RAWG 免费档约 2 万请求/月，每页 1 次请求
REQUEST_INTERVAL = 0.35

DB = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "grecord",
    "charset": "utf8mb4",
}


def load_api_key() -> str:
    if not LOCAL_YML.exists():
        sys.exit(f"缺少 {LOCAL_YML}，请配置 rawg.api-key")
    data = yaml.safe_load(LOCAL_YML.read_text(encoding="utf-8")) or {}
    key = (data.get("rawg") or {}).get("api-key", "").strip()
    if not key:
        sys.exit("application-local.yml 中 rawg.api-key 为空")
    return key


def load_db_config() -> None:
    if not APPLICATION_YML.exists():
        return
    text = APPLICATION_YML.read_text(encoding="utf-8")
    m = re.search(r"jdbc:mysql://(?:[^:/]+):(\d+)/([^?]+)", text)
    if m:
        DB["port"] = int(m.group(1))
        DB["database"] = m.group(2)


def extract_steam_appid(stores: list | None) -> int | None:
    if not stores:
        return None
    for s in stores:
        store = s.get("store") or {}
        if store.get("slug") != "steam":
            continue
        url = s.get("url") or ""
        m = re.search(r"/app/(\d+)", url)
        if m:
            return int(m.group(1))
    return None


def map_row(d: dict) -> dict:
    platforms = ",".join(
        p.get("platform", {}).get("name", "").strip()
        for p in (d.get("platforms") or [])
        if p.get("platform", {}).get("name")
    )
    genres = ",".join(g.get("name", "").strip() for g in (d.get("genres") or []) if g.get("name"))
    devs = d.get("developers") or []
    desc = (d.get("description_raw") or d.get("description") or "").strip()
    if len(desc) > 5000:
        desc = desc[:5000]
    return {
        "external_id": str(d["id"]),
        "name": (d.get("name") or "").strip(),
        "description": desc or None,
        "icon": d.get("background_image"),
        "platforms": platforms,
        "game_types": genres,
        "rating": d.get("rating"),
        "avg_play_time": d.get("playtime"),
        "developer": devs[0]["name"] if devs else None,
        "steam_appid": extract_steam_appid(d.get("stores")),
    }


def ensure_sync_table(conn) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS game_sync_log (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              job_name VARCHAR(64) NOT NULL,
              last_page INT DEFAULT 0,
              total_imported INT DEFAULT 0,
              status VARCHAR(16) DEFAULT 'idle',
              error_message VARCHAR(1000) NULL,
              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              UNIQUE KEY uk_job (job_name)
            )
            """
        )
    conn.commit()


def load_sync_log(conn) -> dict:
    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute("SELECT * FROM game_sync_log WHERE job_name=%s", (JOB_NAME,))
        row = cur.fetchone()
        if row:
            return row
        cur.execute(
            "INSERT INTO game_sync_log (job_name, last_page, total_imported, status) VALUES (%s, 0, 0, 'idle')",
            (JOB_NAME,),
        )
    conn.commit()
    return {"job_name": JOB_NAME, "last_page": 0, "total_imported": 0, "status": "idle"}


def save_sync_log(conn, last_page: int, total_imported: int, status: str, error: str | None = None) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE game_sync_log
            SET last_page=%s, total_imported=%s, status=%s, error_message=%s
            WHERE job_name=%s
            """,
            (last_page, total_imported, status, error, JOB_NAME),
        )
    conn.commit()


def upsert_game(conn, g: dict) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id FROM game
            WHERE external_provider='rawg' AND external_id=%s AND is_delete=0
            LIMIT 1
            """,
            (g["external_id"],),
        )
        row = cur.fetchone()
        if row:
            cur.execute(
                """
                UPDATE game SET
                  name=%s, description=%s, icon=%s, platforms=%s, game_types=%s,
                  rating=%s, avg_play_time=%s, developer=%s, steam_appid=%s,
                  source='rawg', update_time=NOW()
                WHERE id=%s
                """,
                (
                    g["name"], g["description"], g["icon"], g["platforms"], g["game_types"],
                    g["rating"], g["avg_play_time"], g["developer"], g["steam_appid"], row[0],
                ),
            )
        else:
            cur.execute(
                """
                INSERT INTO game (
                  name, description, icon, platforms, game_types, rating, avg_play_time,
                  developer, steam_appid, external_provider, external_id, source,
                  status, is_delete, create_time, update_time
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,'rawg',%s,'rawg',0,0,NOW(),NOW())
                """,
                (
                    g["name"], g["description"], g["icon"], g["platforms"], g["game_types"],
                    g["rating"], g["avg_play_time"], g["developer"], g["steam_appid"], g["external_id"],
                ),
            )
    conn.commit()


MAX_RETRIES = 5
RETRYABLE_STATUS = {429, 502, 503, 504}


def fetch_games_page(session: requests.Session, api_key: str, page: int) -> dict:
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            r = session.get(
                f"{RAWG_BASE}/games",
                params={
                    "key": api_key,
                    "page": page,
                    "page_size": PAGE_SIZE,
                    "ordering": ORDERING,
                },
                headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
                timeout=60,
            )
            if r.status_code in RETRYABLE_STATUS and attempt < MAX_RETRIES:
                wait = min(2**attempt, 30)
                print(f"page {page}: HTTP {r.status_code}，{wait}s 后重试 ({attempt}/{MAX_RETRIES})")
                time.sleep(wait)
                continue
            r.raise_for_status()
            return r.json()
        except requests.RequestException as e:
            if attempt >= MAX_RETRIES:
                raise
            wait = min(2**attempt, 30)
            print(f"page {page}: 请求失败 ({e})，{wait}s 后重试 ({attempt}/{MAX_RETRIES})")
            time.sleep(wait)
    raise RuntimeError(f"page {page}: 重试 {MAX_RETRIES} 次后仍失败")


def import_catalog(
    target_total: int,
    batch_limit: int | None,
    reset: bool,
    max_pages: int | None,
) -> int:
    load_db_config()
    api_key = load_api_key()
    session = requests.Session()
    session.trust_env = True

    conn = pymysql.connect(**DB)
    ensure_sync_table(conn)
    log = load_sync_log(conn)

    if reset:
        save_sync_log(conn, 0, 0, "idle", None)
        log = {"last_page": 0, "total_imported": 0, "status": "idle"}

    if max_pages is not None:
        target_total = max(max_pages * PAGE_SIZE, target_total)

    already = int(log.get("total_imported") or 0)
    last_page = int(log.get("last_page") or 0)

    if max_pages is not None and last_page >= max_pages:
        print(f"已用满 {max_pages} 页 API 配额（last_page={last_page}），无需继续。")
        conn.close()
        return 0

    if max_pages is None and already >= target_total:
        print(f"已达目标 {target_total} 条（当前 {already}），无需导入。使用 --reset 可重来。")
        conn.close()
        return 0

    page = last_page + 1
    imported_this_run = 0
    save_sync_log(conn, last_page, already, "running", None)

    try:
        while True:
            if max_pages is not None and page > max_pages:
                save_sync_log(conn, max_pages, already + imported_this_run, "done", None)
                print(f"已达本月 max-pages={max_pages}，停止。")
                break

            if max_pages is None and already + imported_this_run >= target_total:
                break

            if batch_limit is not None and imported_this_run >= batch_limit:
                save_sync_log(conn, page - 1, already + imported_this_run, "idle", None)
                break

            data = fetch_games_page(session, api_key, page)
            results = data.get("results") or []
            if not results:
                print("无更多结果，结束。")
                save_sync_log(conn, page - 1, already + imported_this_run, "done", None)
                break

            for item in results:
                if not item.get("id") or not item.get("name"):
                    continue
                upsert_game(conn, map_row(item))
                imported_this_run += 1
                if batch_limit is not None and imported_this_run >= batch_limit:
                    break
                if max_pages is None and already + imported_this_run >= target_total:
                    break

            total_now = already + imported_this_run
            quota_hint = f"/{max_pages}页" if max_pages else f"/{target_total}条"
            print(f"page {page}{quota_hint}: 本页 {len(results)} 条，累计 {total_now} 条")
            status = "idle"
            if max_pages is not None and page >= max_pages:
                status = "done"
            elif max_pages is None and total_now >= target_total:
                status = "done"
            save_sync_log(conn, page, total_now, status, None)
            page += 1

            if not data.get("next"):
                print("RAWG 返回 next=null，已到末页。")
                save_sync_log(conn, page - 1, total_now, "done", None)
                break

            time.sleep(REQUEST_INTERVAL)

    except Exception as e:
        save_sync_log(conn, max(page - 1, 0), already + imported_this_run, "failed", str(e)[:1000])
        conn.close()
        raise

    conn.close()
    api_used = int(log.get("last_page") or 0) + (page - 1 - last_page)
    print(
        f"本次导入 {imported_this_run} 条，库内累计约 {already + imported_this_run} 条"
        + (f"，本 run 消耗约 {page - last_page - 1} 次 API" if page > last_page + 1 else "")
    )
    return imported_this_run


def main() -> None:
    parser = argparse.ArgumentParser(description="RAWG 热门游戏目录导入 MySQL")
    parser.add_argument("--target", type=int, default=DEFAULT_TARGET, help="总目标条数（与 --max-pages 二选一）")
    parser.add_argument(
        "--max-pages",
        type=int,
        default=None,
        help=f"最多请求页数（1页=1次API，免费档约 {MONTHLY_MAX_PAGES}/月）",
    )
    parser.add_argument("--batch", type=int, default=None, help="本次最多导入条数")
    parser.add_argument("--reset", action="store_true", help="重置断点从第 1 页开始")
    args = parser.parse_args()
    import_catalog(args.target, args.batch, args.reset, args.max_pages)


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    main()
