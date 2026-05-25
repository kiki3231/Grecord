import pymysql

conn = pymysql.connect(host='127.0.0.1', user='root', password='123456',
                       database='grecord', charset='utf8mb4', autocommit=False)
cur = conn.cursor()

# 1. 确认新列数据
print("=== 前3条游戏的多值字段 ===")
cur.execute("SELECT name, platforms, game_types, icon FROM game LIMIT 3")
for row in cur.fetchall():
    print(f"游戏: {row[0][:25]}")
    print(f"  platforms : {row[1]}")
    print(f"  game_types: {row[2]}")
    print(f"  icon      : {row[3]}")
    print()

# 2. 查外键约束名
print("=== game 表的外键约束 ===")
cur.execute("""
    SELECT CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA='grecord' AND TABLE_NAME='game'
    AND REFERENCED_TABLE_NAME IS NOT NULL
""")
fks = cur.fetchall()
for fk in fks:
    print(f"  约束名: {fk[0]}, 列: {fk[1]}, 引用表: {fk[2]}")

# 3. 删除外键约束，再删除旧列
print("\n=== 清理废弃的 type_id / platform_id ===")
for fk_name, col_name, _ in fks:
    try:
        cur.execute(f"ALTER TABLE game DROP FOREIGN KEY `{fk_name}`")
        conn.commit()
        print(f"  OK: 删除外键约束 {fk_name}")
    except Exception as e:
        conn.rollback()
        print(f"  跳过外键 {fk_name}: {e}")

for col in ("type_id", "platform_id"):
    try:
        cur.execute(f"ALTER TABLE game DROP COLUMN `{col}`")
        conn.commit()
        print(f"  OK: 删除列 {col}")
    except Exception as e:
        conn.rollback()
        print(f"  跳过列 {col}: {e}")

print("\n===== 完成 =====")
conn.close()
