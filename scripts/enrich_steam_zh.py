#!/usr/bin/env python3
"""
从 Steam 国区商店写入 name_zh（官方简体名，非机翻）。

用法:
  python scripts/enrich_steam_zh.py --limit 5000
  python scripts/enrich_steam_zh.py --limit 5000 --reset
"""
from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import requests

from catalog_common import (
    configure_stdout,
    connect,
    ensure_sync_table,
    fetch_json_with_retry,
    has_cjk,
    load_sync_log,
    save_sync_log,
)

JOB_NAME = "steam_name_zh"
STEAM_API = "https://store.steampowered.com/api/appdetails"
STEAM_INTERVAL = 2.5


def fetch_pending_batch(conn, batch_size: int, top_limit: int | None) -> list[dict]:
    """待写入 name_zh 的游戏；每批重新查询，已写入的会自动排除。"""
    if top_limit is not None:
        sql = """
            SELECT id, name, steam_appid
            FROM (
              SELECT id, name, name_zh, steam_appid, rating
              FROM game
              WHERE source='rawg' AND is_delete=0
              ORDER BY rating DESC, id DESC
              LIMIT %s
            ) topn
            WHERE steam_appid IS NOT NULL
              AND name_zh IS NULL
            ORDER BY rating DESC, id DESC
            LIMIT %s
        """
        params = (top_limit, batch_size)
    else:
        sql = """
            SELECT id, name, steam_appid
            FROM game
            WHERE is_delete=0
              AND steam_appid IS NOT NULL
              AND name_zh IS NULL
            ORDER BY rating DESC, id DESC
            LIMIT %s
        """
        params = (batch_size,)
    with conn.cursor() as cur:
        cur.execute(sql, params)
        cols = [d[0] for d in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]


def mark_steam_checked(conn, game_id: int, name_zh: str | None) -> None:
    """有中文则写入；无中文则写空串表示已查过（非机翻）。"""
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE game SET name_zh=%s, update_time=NOW() WHERE id=%s",
            (name_zh if name_zh else "", game_id),
        )
    conn.commit()


def fetch_steam_schinese(session: requests.Session, appid: int) -> str | None:
    data = fetch_json_with_retry(
        session,
        STEAM_API,
        params={"appids": appid, "l": "schinese", "cc": "cn"},
        label=f"steam={appid}",
    )
    entry = data.get(str(appid)) or {}
    if not entry.get("success"):
        return None
    name = (entry.get("data") or {}).get("name") or ""
    name = name.strip()
    if not name or not has_cjk(name):
        return None
    return name


def enrich(top_limit: int | None, reset: bool) -> None:
    session = requests.Session()
    session.trust_env = True

    conn = connect()
    ensure_sync_table(conn)
    log = load_sync_log(conn, JOB_NAME)

    processed = 0 if reset else int(log.get("last_page") or 0)
    enriched_total = 0 if reset else int(log.get("total_imported") or 0)
    if reset:
        save_sync_log(conn, JOB_NAME, 0, 0, "idle", None)

    enriched_this_run = 0
    save_sync_log(conn, JOB_NAME, processed, enriched_total, "running", None)

    try:
        while True:
            rows = fetch_pending_batch(conn, 50, top_limit)
            if not rows:
                scope = f"Top {top_limit}" if top_limit else "全库"
                print(f"{scope} 无更多待写入 name_zh 的 Steam 游戏。")
                break

            for row in rows:
                gid = row["id"]
                appid = row["steam_appid"]
                en_name = row["name"]

                name_zh = fetch_steam_schinese(session, appid)
                mark_steam_checked(conn, gid, name_zh)
                if name_zh:
                    enriched_this_run += 1
                    enriched_total += 1
                    print(f"[+{enriched_this_run}] {en_name[:35]} → {name_zh}")
                else:
                    print(f"[—] {en_name[:35]} → Steam 国区无汉字名，跳过")

                processed += 1
                save_sync_log(conn, JOB_NAME, processed, enriched_total, "running", None)
                time.sleep(STEAM_INTERVAL)

        save_sync_log(conn, JOB_NAME, processed, enriched_total, "done", None)
        print(
            f"完成：请求 {processed} 条，本次写入 name_zh {enriched_this_run} 条，累计 {enriched_total} 条。"
        )
    except Exception as e:
        save_sync_log(conn, JOB_NAME, processed, enriched_total, "failed", str(e)[:1000])
        conn.close()
        raise
    conn.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="Steam 国区官方名 → name_zh")
    parser.add_argument("--limit", type=int, default=None, help="仅处理 rating Top N（默认全库有 steam_appid 的）")
    parser.add_argument("--reset", action="store_true", help="重置断点计数")
    args = parser.parse_args()
    enrich(args.limit, args.reset)


if __name__ == "__main__":
    configure_stdout()
    main()
