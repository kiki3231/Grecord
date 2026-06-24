#!/usr/bin/env python3
"""
豆瓣游戏与 RAWG 目录去重：
1. 名称完全一致（忽略大小写/首尾空格）→ 合并关联到 RAWG 条目，软删豆瓣
2. 无任何用户关联（打卡/想玩）的豆瓣条目 → 软删

用法: python scripts/dedup_douban_games.py
       python scripts/dedup_douban_games.py --dry-run
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import pymysql
import yaml

ROOT = Path(__file__).resolve().parent.parent
APPLICATION_YML = ROOT / "backend" / "src" / "main" / "resources" / "application.yml"

DB = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "grecord",
    "charset": "utf8mb4",
}


def load_db_config() -> None:
    if not APPLICATION_YML.exists():
        return
    text = APPLICATION_YML.read_text(encoding="utf-8")
    m = re.search(r"jdbc:mysql://(?:[^:/]+):(\d+)/([^?]+)", text)
    if m:
        DB["port"] = int(m.group(1))
        DB["database"] = m.group(2)


def is_referenced(conn, game_id: int) -> bool:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT 1 FROM game_record WHERE game_id=%s AND is_delete=0 LIMIT 1",
            (game_id,),
        )
        if cur.fetchone():
            return True
        cur.execute(
            "SELECT 1 FROM game_backlog WHERE game_id=%s AND is_delete=0 LIMIT 1",
            (game_id,),
        )
        return cur.fetchone() is not None


def merge_references(conn, from_id: int, to_id: int, dry_run: bool) -> None:
    if from_id == to_id:
        return

    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute(
            "SELECT id, user_id FROM game_backlog WHERE game_id=%s AND is_delete=0",
            (from_id,),
        )
        backlogs = cur.fetchall()
        for row in backlogs:
            cur.execute(
                """
                SELECT id FROM game_backlog
                WHERE user_id=%s AND game_id=%s AND is_delete=0 LIMIT 1
                """,
                (row["user_id"], to_id),
            )
            if cur.fetchone():
                if not dry_run:
                    cur.execute("UPDATE game_backlog SET is_delete=1 WHERE id=%s", (row["id"],))
            elif not dry_run:
                cur.execute(
                    "UPDATE game_backlog SET game_id=%s WHERE id=%s",
                    (to_id, row["id"]),
                )

        cur.execute(
            "SELECT id, user_id, record_date FROM game_record WHERE game_id=%s AND is_delete=0",
            (from_id,),
        )
        records = cur.fetchall()
        for row in records:
            cur.execute(
                """
                SELECT id FROM game_record
                WHERE user_id=%s AND game_id=%s AND record_date=%s AND is_delete=0 LIMIT 1
                """,
                (row["user_id"], to_id, row["record_date"]),
            )
            if cur.fetchone():
                if not dry_run:
                    cur.execute("UPDATE game_record SET is_delete=1 WHERE id=%s", (row["id"],))
            elif not dry_run:
                cur.execute(
                    "UPDATE game_record SET game_id=%s WHERE id=%s",
                    (to_id, row["id"]),
                )

    if not dry_run:
        conn.commit()


def soft_delete_game(conn, game_id: int, dry_run: bool) -> None:
    if dry_run:
        return
    with conn.cursor() as cur:
        cur.execute("UPDATE game SET is_delete=1, update_time=NOW() WHERE id=%s", (game_id,))
    conn.commit()


def dedup(dry_run: bool) -> None:
    load_db_config()
    conn = pymysql.connect(**DB)

    merged = 0
    removed_unreferenced = 0
    removed_orphan_after_merge = 0

    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute(
            """
            SELECT d.id AS douban_id, d.name AS douban_name, r.id AS rawg_id, r.name AS rawg_name
            FROM game d
            INNER JOIN game r
              ON LOWER(TRIM(d.name)) = LOWER(TRIM(r.name))
             AND r.source = 'rawg' AND r.is_delete = 0
            WHERE d.source = 'douban' AND d.is_delete = 0
            """
        )
        pairs = cur.fetchall()

    print(f"名称完全一致可合并: {len(pairs)} 对")
    for p in pairs:
        print(f"  合并 {p['douban_id']}「{p['douban_name']}」→ RAWG {p['rawg_id']}")
        merge_references(conn, p["douban_id"], p["rawg_id"], dry_run)
        soft_delete_game(conn, p["douban_id"], dry_run)
        merged += 1

    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute(
            "SELECT id, name FROM game WHERE source='douban' AND is_delete=0"
        )
        remaining = cur.fetchall()

    for row in remaining:
        gid = row["id"]
        if is_referenced(conn, gid):
            print(f"  保留（有用户数据）豆瓣 id={gid}「{row['name']}」")
            continue
        print(f"  软删无关联豆瓣 id={gid}「{row['name']}」")
        soft_delete_game(conn, gid, dry_run)
        removed_unreferenced += 1

    print(
        f"\n{'[dry-run] ' if dry_run else ''}完成: "
        f"合并 {merged} 对, 软删无关联豆瓣 {removed_unreferenced} 条"
    )
    conn.close()


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except Exception:
            pass
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="仅打印不写入")
    args = parser.parse_args()
    dedup(args.dry_run)


if __name__ == "__main__":
    main()
