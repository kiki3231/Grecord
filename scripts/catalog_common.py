"""游戏目录脚本共用：数据库、RAWG Key、同步断点、Steam AppID 提取。"""
from __future__ import annotations

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

RAWG_BASE = "https://api.rawg.io/api"
USER_AGENT = "GRecord/1.0 (catalog-import; +https://rawg.io/apidocs)"
MAX_RETRIES = 5
RETRYABLE_STATUS = {429, 502, 503, 504}

DB = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "grecord",
    "charset": "utf8mb4",
}


def configure_stdout() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")


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


def connect():
    load_db_config()
    return pymysql.connect(**DB)


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


def load_sync_log(conn, job_name: str) -> dict:
    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute("SELECT * FROM game_sync_log WHERE job_name=%s", (job_name,))
        row = cur.fetchone()
        if row:
            return row
        cur.execute(
            "INSERT INTO game_sync_log (job_name, last_page, total_imported, status) VALUES (%s, 0, 0, 'idle')",
            (job_name,),
        )
    conn.commit()
    return {"job_name": job_name, "last_page": 0, "total_imported": 0, "status": "idle"}


def save_sync_log(
    conn,
    job_name: str,
    last_page: int,
    total_imported: int,
    status: str,
    error: str | None = None,
) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE game_sync_log
            SET last_page=%s, total_imported=%s, status=%s, error_message=%s
            WHERE job_name=%s
            """,
            (last_page, total_imported, status, error, job_name),
        )
    conn.commit()


def extract_steam_appid(stores: list | None) -> int | None:
    if not stores:
        return None
    for s in stores:
        url = s.get("url") or ""
        store = s.get("store") or {}
        is_steam = store.get("slug") == "steam" or s.get("store_id") == 1 or "steampowered.com" in url
        if not is_steam:
            continue
        m = re.search(r"/app/(\d+)", url)
        if m:
            return int(m.group(1))
    return None


def fetch_rawg_stores(session: requests.Session, api_key: str, external_id: str) -> list:
    """RAWG 游戏详情里的 stores.url 常为空，需调 /games/{id}/stores。"""
    data = fetch_json_with_retry(
        session,
        f"{RAWG_BASE}/games/{external_id}/stores",
        params={"key": api_key},
        label=f"rawg-stores={external_id}",
    )
    return data.get("results") or []


def fetch_json_with_retry(
    session: requests.Session,
    url: str,
    *,
    params: dict | None = None,
    label: str = "",
) -> dict:
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            r = session.get(
                url,
                params=params,
                headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
                timeout=60,
            )
            if r.status_code in RETRYABLE_STATUS and attempt < MAX_RETRIES:
                wait = min(15 * attempt, 90) if r.status_code == 429 else min(2**attempt, 30)
                print(f"{label}: HTTP {r.status_code}，{wait}s 后重试 ({attempt}/{MAX_RETRIES})")
                time.sleep(wait)
                continue
            r.raise_for_status()
            return r.json()
        except requests.RequestException as e:
            if attempt >= MAX_RETRIES:
                raise
            wait = min(2**attempt, 30)
            print(f"{label}: 请求失败 ({e})，{wait}s 后重试 ({attempt}/{MAX_RETRIES})")
            time.sleep(wait)
    raise RuntimeError(f"{label}: 重试 {MAX_RETRIES} 次后仍失败")


def has_cjk(text: str) -> bool:
    return bool(re.search(r"[\u4e00-\u9fff]", text or ""))


def extract_chinese_from_mixed_name(name: str) -> str | None:
    """从豆瓣等「中文 + 英文/日文」混合标题中提取中文段（非翻译）。"""
    if not name or not has_cjk(name):
        return None
    # 在首个假名片段之前截取，避免把日文汉字误入中文名
    head = re.split(r"[\u3040-\u30ff\u31f0-\u31ff]", name, maxsplit=1)[0]
    head = re.split(r"\s{2,}|\s+(?=[A-Za-z0-9])", head.strip(), maxsplit=1)[0]
    parts = re.findall(r"[\u4e00-\u9fff]+", head)
    if not parts:
        return None
    return " ".join(parts).strip() or None
