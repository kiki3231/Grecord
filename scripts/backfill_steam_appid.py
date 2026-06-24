#!/usr/bin/env python3
"""
为热门 RAWG 游戏回填 steam_appid（RAWG detail → stores，非翻译）。

用法:
  python scripts/backfill_steam_appid.py --limit 5000
  python scripts/backfill_steam_appid.py --limit 5000 --reset
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
    extract_steam_appid,
    fetch_rawg_stores,
    load_api_key,
    load_sync_log,
    save_sync_log,
)

JOB_NAME = "rawg_steam_appid_top5000"
RAWG_INTERVAL = 0.35


def fetch_top_games(conn, limit: int, offset: int) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, external_id, name, steam_appid, rating
            FROM game
            WHERE source='rawg' AND is_delete=0 AND external_id IS NOT NULL
            ORDER BY rating DESC, id DESC
            LIMIT %s OFFSET %s
            """,
            (limit, offset),
        )
        cols = [d[0] for d in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]


def update_steam_appid(conn, game_id: int, steam_appid: int | None) -> None:
    with conn.cursor() as cur:
        cur.execute(
            "UPDATE game SET steam_appid=%s, update_time=NOW() WHERE id=%s",
            (steam_appid, game_id),
        )
    conn.commit()


def backfill(limit: int, reset: bool) -> None:
    api_key = load_api_key()
    session = requests.Session()
    session.trust_env = True

    conn = connect()
    ensure_sync_table(conn)
    log = load_sync_log(conn, JOB_NAME)

    offset = 0 if reset else int(log.get("last_page") or 0)
    found_total = int(log.get("total_imported") or 0)
    if reset:
        save_sync_log(conn, JOB_NAME, 0, 0, "idle", None)

    if offset >= limit:
        print(f"已达 --limit={limit}（offset={offset}），无需继续。")
        conn.close()
        return

    processed = 0
    found_this_run = 0
    save_sync_log(conn, JOB_NAME, offset, found_total, "running", None)

    try:
        while offset < limit:
            batch_size = min(50, limit - offset)
            rows = fetch_top_games(conn, batch_size, offset)
            if not rows:
                print("没有更多游戏。")
                break

            for row in rows:
                gid = row["id"]
                ext_id = row["external_id"]
                name = row["name"]
                existing = row.get("steam_appid")

                if existing:
                    processed += 1
                    offset += 1
                    continue

                stores = fetch_rawg_stores(session, api_key, ext_id)
                steam_id = extract_steam_appid(stores)
                update_steam_appid(conn, gid, steam_id)
                processed += 1
                offset += 1
                if steam_id:
                    found_this_run += 1
                    found_total += 1

                print(
                    f"[{offset}/{limit}] {name[:40]} → steam_appid={steam_id or '无'}"
                )
                save_sync_log(conn, JOB_NAME, offset, found_total, "running", None)
                time.sleep(RAWG_INTERVAL)

            if len(rows) < batch_size:
                break

        save_sync_log(conn, JOB_NAME, offset, found_total, "done", None)
        print(f"完成：处理 {processed} 条，本次新发现 Steam {found_this_run} 条，累计 {found_total} 条。")
    except Exception as e:
        save_sync_log(conn, JOB_NAME, offset, found_total, "failed", str(e)[:1000])
        conn.close()
        raise
    conn.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="RAWG detail 回填 steam_appid")
    parser.add_argument("--limit", type=int, default=5000, help="按 rating 取前 N 条")
    parser.add_argument("--reset", action="store_true", help="从第 1 条重新开始")
    args = parser.parse_args()
    backfill(args.limit, args.reset)


if __name__ == "__main__":
    configure_stdout()
    main()
