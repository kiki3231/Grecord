"""
游戏数据导入脚本
1. 执行 migration.sql 更新数据库结构
2. 填充 game_type / game_platform 表
3. 批量导入 game 表
"""
import json
import re
from datetime import datetime

try:
    import pymysql
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pymysql"])
    import pymysql

# ===== 数据库连接配置（与 application.yml 保持一致） =====
DB_CONFIG = dict(
    host="127.0.0.1",
    port=3306,
    user="root",
    password="123456",
    database="grecord",
    charset="utf8mb4",
    autocommit=False,
)

DATA_FILE = "游戏数据_本地封面.txt"
MIGRATION_FILE = r"D:\GRecord\backend\src\main\resources\sql\migration.sql"
NOW = datetime.now().strftime("%Y-%m-%d %H:%M:%S")


# ---------- 工具函数 ----------

def normalize_list(raw: str, sep_pattern: str) -> list[str]:
    """按分隔符拆分并去除空白"""
    parts = re.split(sep_pattern, raw or "")
    return [p.strip() for p in parts if p.strip()]


def column_exists(conn, table: str, column: str) -> bool:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s AND COLUMN_NAME = %s",
            (table, column),
        )
        return cur.fetchone()[0] > 0


def index_exists(conn, table: str, index_name: str) -> bool:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT COUNT(*) FROM information_schema.STATISTICS "
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s AND INDEX_NAME = %s",
            (table, index_name),
        )
        return cur.fetchone()[0] > 0


def run_migration(conn):
    """兼容所有 MySQL 版本的迁移：通过 information_schema 判断后再执行"""
    print(">>> 执行数据库迁移...")

    def exec_sql(sql: str, desc: str):
        with conn.cursor() as cur:
            try:
                cur.execute(sql)
                conn.commit()
                print(f"  OK: {desc}")
            except Exception as e:
                conn.rollback()
                print(f"  跳过: {desc} ({e})")

    # --- game 表 ---
    # 删除旧外键列
    if column_exists(conn, "game", "type_id"):
        exec_sql("ALTER TABLE game DROP COLUMN type_id", "game.DROP type_id")
    if column_exists(conn, "game", "platform_id"):
        exec_sql("ALTER TABLE game DROP COLUMN platform_id", "game.DROP platform_id")

    # 新增字段
    if not column_exists(conn, "game", "platforms"):
        exec_sql("ALTER TABLE game ADD COLUMN platforms VARCHAR(255) COMMENT '平台，逗号分隔'", "game.ADD platforms")
    if not column_exists(conn, "game", "game_types"):
        exec_sql("ALTER TABLE game ADD COLUMN game_types VARCHAR(255) COMMENT '类型，逗号分隔'", "game.ADD game_types")
    if not column_exists(conn, "game", "avg_play_time"):
        exec_sql("ALTER TABLE game ADD COLUMN avg_play_time INT COMMENT '平均通关时长(分钟)'", "game.ADD avg_play_time")
    if not column_exists(conn, "game", "rating"):
        exec_sql("ALTER TABLE game ADD COLUMN rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '均分'", "game.ADD rating")
    if not column_exists(conn, "game", "review"):
        exec_sql("ALTER TABLE game ADD COLUMN review VARCHAR(1000) COMMENT '精选评论'", "game.ADD review")
    if not column_exists(conn, "game", "developer"):
        exec_sql("ALTER TABLE game ADD COLUMN developer VARCHAR(128) COMMENT '开发商'", "game.ADD developer")
    if not column_exists(conn, "game", "source"):
        exec_sql("ALTER TABLE game ADD COLUMN source VARCHAR(32) DEFAULT 'user' COMMENT '数据来源'", "game.ADD source")

    # --- game_record 表 ---
    if not column_exists(conn, "game_record", "rating"):
        exec_sql("ALTER TABLE game_record ADD COLUMN rating DECIMAL(2,1) COMMENT '用户评分 1.0-10.0'", "game_record.ADD rating")
    if not column_exists(conn, "game_record", "status"):
        exec_sql("ALTER TABLE game_record ADD COLUMN status TINYINT DEFAULT 1 COMMENT '1在玩 2已通关 3已搁置 4白金'", "game_record.ADD status")

    # --- game_backlog 表 ---
    exec_sql("""
        CREATE TABLE IF NOT EXISTS game_backlog (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          user_id BIGINT NOT NULL,
          game_id BIGINT NOT NULL,
          status TINYINT DEFAULT 0,
          priority TINYINT DEFAULT 1,
          sort_order INT DEFAULT 0,
          notes VARCHAR(500),
          started_date DATETIME,
          completed_date DATETIME,
          create_by BIGINT,
          update_by BIGINT,
          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          is_delete TINYINT DEFAULT 0,
          UNIQUE KEY uk_user_game (user_id, game_id, is_delete)
        ) COMMENT '游戏清单表'
    """, "CREATE TABLE game_backlog")

    # --- 索引 ---
    if not index_exists(conn, "game", "idx_game_name"):
        exec_sql("CREATE INDEX idx_game_name ON game(name)", "INDEX idx_game_name")
    if not index_exists(conn, "game", "idx_game_source"):
        exec_sql("CREATE INDEX idx_game_source ON game(source)", "INDEX idx_game_source")
    if not index_exists(conn, "game_record", "idx_record_user_date"):
        exec_sql("CREATE INDEX idx_record_user_date ON game_record(user_id, record_date)", "INDEX idx_record_user_date")
    if not index_exists(conn, "game_record", "idx_record_game"):
        exec_sql("CREATE INDEX idx_record_game ON game_record(game_id)", "INDEX idx_record_game")
    if not index_exists(conn, "game_backlog", "idx_backlog_user_status"):
        exec_sql("CREATE INDEX idx_backlog_user_status ON game_backlog(user_id, status)", "INDEX idx_backlog_user_status")

    print(">>> 迁移完成\n")


def upsert_ref_table(conn, table: str, names: list[str]) -> dict[str, int]:
    """向 game_type 或 game_platform 表插入不存在的名称，返回 name->id 映射"""
    name_to_id: dict[str, int] = {}
    with conn.cursor() as cur:
        # 先查已有的
        cur.execute(f"SELECT id, name FROM `{table}`")
        for row in cur.fetchall():
            name_to_id[row[1]] = row[0]

        for name in names:
            if name not in name_to_id:
                cur.execute(
                    f"INSERT INTO `{table}` (name, create_time, update_time, is_delete) "
                    f"VALUES (%s, %s, %s, 0)",
                    (name, NOW, NOW),
                )
                name_to_id[name] = cur.lastrowid
    conn.commit()
    return name_to_id


def import_games(conn, games: list[dict]):
    """批量插入游戏数据"""
    insert_sql = """
        INSERT INTO game
            (name, description, icon, platforms, game_types,
             rating, review, source, status, is_delete, create_time, update_time)
        VALUES
            (%s, %s, %s, %s, %s,
             %s, %s, %s, 1, 0, %s, %s)
    """
    rows = []
    for g in games:
        # 平台：用「、」分隔，统一转为逗号
        platforms_raw = g.get("platform", "") or ""
        platforms = ",".join(normalize_list(platforms_raw, r"[、,，]"))

        # 类型：用「/」分隔，统一转为逗号
        types_raw = g.get("game_type", "") or ""
        game_types = ",".join(normalize_list(types_raw, r"\s*/\s*"))

        # 评分
        try:
            rating = float(g.get("rating") or 0)
        except ValueError:
            rating = 0.0

        rows.append((
            (g.get("name") or "")[:100],
            g.get("intro") or "",
            g.get("cover") or "",
            platforms[:255],
            game_types[:255],
            rating,
            (g.get("review") or "")[:1000],
            "douban",
            NOW,
            NOW,
        ))

    with conn.cursor() as cur:
        cur.executemany(insert_sql, rows)
    conn.commit()
    return len(rows)


def collect_unique(games: list[dict], field: str, sep_pattern: str) -> list[str]:
    """收集所有唯一的分类/平台名称"""
    seen = set()
    result = []
    for g in games:
        for item in normalize_list(g.get(field, "") or "", sep_pattern):
            if item not in seen:
                seen.add(item)
                result.append(item)
    return result


# ---------- 主流程 ----------

def main():
    print(f"读取数据文件: {DATA_FILE}")
    with open(DATA_FILE, encoding="utf-8") as f:
        games = json.load(f)
    print(f"共 {len(games)} 条游戏数据\n")

    conn = pymysql.connect(**DB_CONFIG)
    try:
        # 1. 执行迁移
        run_migration(conn)

        # 2. 填充 game_type 表
        unique_types = collect_unique(games, "game_type", r"\s*/\s*")
        print(f">>> 填充 game_type 表，共 {len(unique_types)} 种类型...")
        upsert_ref_table(conn, "game_type", unique_types)
        print(">>> game_type 完成\n")

        # 3. 填充 game_platform 表
        unique_platforms = collect_unique(games, "platform", r"[、,，]")
        print(f">>> 填充 game_platform 表，共 {len(unique_platforms)} 种平台...")
        upsert_ref_table(conn, "game_platform", unique_platforms)
        print(">>> game_platform 完成\n")

        # 4. 导入 game 表
        print(">>> 开始导入 game 表...")
        count = import_games(conn, games)
        print(f">>> 成功导入 {count} 条游戏数据\n")

    except Exception as e:
        conn.rollback()
        print(f"\n[错误] {e}")
        raise
    finally:
        conn.close()

    print("===== 全部完成 =====")


if __name__ == "__main__":
    main()
