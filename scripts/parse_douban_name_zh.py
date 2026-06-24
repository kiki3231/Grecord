#!/usr/bin/env python3
"""从豆瓣混合标题解析中文名写入 name_zh（非翻译）。"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from catalog_common import configure_stdout, connect, extract_chinese_from_mixed_name


def main() -> None:
    conn = connect()
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, name FROM game
            WHERE source='douban' AND is_delete=0
            """
        )
        rows = cur.fetchall()

    updated = 0
    for gid, name in rows:
        zh = extract_chinese_from_mixed_name(name or "")
        if not zh:
            continue
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE game SET name_zh=%s, update_time=NOW() WHERE id=%s",
                (zh, gid),
            )
        conn.commit()
        updated += 1
        print(f"id={gid}: {zh}")

    conn.close()
    print(f"豆瓣 name_zh 更新 {updated} 条。")


if __name__ == "__main__":
    configure_stdout()
    main()
