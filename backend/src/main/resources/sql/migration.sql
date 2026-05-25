-- GRecord 数据库迁移脚本
-- 修改 game 表：移除 type_id/platform_id，新增 platforms/game_types 等字段
ALTER TABLE game
  DROP COLUMN IF EXISTS type_id,
  DROP COLUMN IF EXISTS platform_id,
  ADD COLUMN IF NOT EXISTS platforms VARCHAR(255) COMMENT '平台，逗号分隔',
  ADD COLUMN IF NOT EXISTS game_types VARCHAR(255) COMMENT '类型，逗号分隔',
  ADD COLUMN IF NOT EXISTS avg_play_time INT COMMENT '平均通关时长(分钟)',
  ADD COLUMN IF NOT EXISTS rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '均分',
  ADD COLUMN IF NOT EXISTS review VARCHAR(1000) COMMENT '精选评论',
  ADD COLUMN IF NOT EXISTS developer VARCHAR(128) COMMENT '开发商',
  ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'user' COMMENT '数据来源: douban/igdb/user';

-- 修改 game_record 表：新增 rating 和 status 字段
ALTER TABLE game_record
  ADD COLUMN IF NOT EXISTS rating DECIMAL(3,1) COMMENT '用户评分 1.0-10.0',
  ADD COLUMN IF NOT EXISTS status TINYINT DEFAULT 1 COMMENT '1在玩 2已通关 3已搁置 4白金';

-- 修复 game_record.rating 列精度（DECIMAL(2,1) 最大只能存 9.9，评分 10.0 会溢出）
ALTER TABLE game_record MODIFY COLUMN rating DECIMAL(3,1) COMMENT '用户评分 1.0-10.0';

-- 新建 game_backlog 表
CREATE TABLE IF NOT EXISTS game_backlog (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  game_id BIGINT NOT NULL COMMENT '游戏ID',
  status TINYINT DEFAULT 0 COMMENT '0想玩 1在玩 2已完成 3已搁置',
  priority TINYINT DEFAULT 1 COMMENT '0高 1中 2低',
  sort_order INT DEFAULT 0 COMMENT '拖拽排序序号',
  notes VARCHAR(500) COMMENT '备注',
  started_date DATETIME COMMENT '开始玩日期',
  completed_date DATETIME COMMENT '完成日期',
  create_by BIGINT,
  update_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT DEFAULT 0,
  UNIQUE KEY uk_user_game (user_id, game_id, is_delete)
) COMMENT '游戏清单表';

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_game_name ON game(name);
CREATE INDEX IF NOT EXISTS idx_game_source ON game(source);
CREATE INDEX IF NOT EXISTS idx_record_user_date ON game_record(user_id, record_date);
CREATE INDEX IF NOT EXISTS idx_record_game ON game_record(game_id);
CREATE INDEX IF NOT EXISTS idx_backlog_user_status ON game_backlog(user_id, status);
