"""
游戏数据导入脚本
读取 游戏信息爬取/游戏数据.txt（JSON数组），去重后批量写入 MySQL game 表。
用法: python scripts/import_games.py
依赖: pip install pymysql
"""

import json
import sys
import os
from datetime import datetime

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "grecord",
    "charset": "utf8mb4"
}

DATA_FILE = os.path.join(os.path.dirname(__file__), "..", "游戏信息爬取", "游戏数据.txt")

FIELD_MAP = {
    "name": "name",
    "intro": "description",
    "cover": "icon",
    "platform": "platforms",
    "game_type": "game_types",
    "rating": "rating",
    "review": "review",
}

INSERT_SQL = """
INSERT INTO game (name, description, icon, platforms, game_types, rating, review, source, status, is_delete, create_time, update_time)
VALUES (%s, %s, %s, %s, %s, %s, %s, 'douban', 0, 0, %s, %s)
"""

def load_data(filepath: str) -> list:
    print(f"读取数据文件: {filepath}")
    with open(filepath, "r", encoding="utf-8") as f:
        raw = f.read()
    data = json.loads(raw)
    print(f"共读取 {len(data)} 条记录")
    return data

def deduplicate(games: list) -> list:
    seen = set()
    unique = []
    for g in games:
        name = g.get("name", "").strip()
        if not name:
            continue
        if name in seen:
            continue
        seen.add(name)
        unique.append(g)
    print(f"去重后: {len(unique)} 条")
    return unique

def parse_rating(val) -> float:
    if val is None:
        return 0.0
    try:
        return round(float(str(val).strip()), 1)
    except (ValueError, TypeError):
        return 0.0

def truncate(s: str, maxlen: int) -> str:
    if s and len(s) > maxlen:
        return s[:maxlen]
    return s

def import_games():
    if not os.path.exists(DATA_FILE):
        print(f"数据文件不存在: {DATA_FILE}")
        sys.exit(1)

    games = load_data(DATA_FILE)
    games = deduplicate(games)

    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    existing_names_sql = "SELECT name FROM game WHERE source = 'douban' AND is_delete = 0"
    cursor.execute(existing_names_sql)
    existing_names = {row[0] for row in cursor.fetchall()}
    print(f"数据库已有 {len(existing_names)} 条豆瓣游戏记录")

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    inserted = 0
    skipped = 0

    for g in games:
        name = g.get("name", "").strip()
        if name in existing_names:
            skipped += 1
            continue

        description = truncate(g.get("intro", ""), 5000)
        icon = g.get("cover", "")
        platforms = g.get("platform", "")
        game_types = g.get("game_type", "")
        rating = parse_rating(g.get("rating"))
        review = truncate(g.get("review", ""), 1000)

        try:
            cursor.execute(INSERT_SQL, (name, description, icon, platforms, game_types, rating, review, now, now))
            inserted += 1
        except pymysql.IntegrityError:
            skipped += 1
        except Exception as e:
            print(f"  插入失败 [{name}]: {e}")
            skipped += 1

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\n导入完成: 新增 {inserted} 条, 跳过 {skipped} 条")

if __name__ == "__main__":
    import_games()
