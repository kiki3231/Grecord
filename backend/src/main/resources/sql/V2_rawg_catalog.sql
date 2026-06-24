-- RAWG 游戏目录：外部 ID、中文名、别名与同步断点
ALTER TABLE game
  ADD COLUMN IF NOT EXISTS external_provider VARCHAR(16) NULL COMMENT 'rawg/steam/user',
  ADD COLUMN IF NOT EXISTS external_id VARCHAR(64) NULL COMMENT '外部主键',
  ADD COLUMN IF NOT EXISTS name_zh VARCHAR(255) NULL COMMENT '中文名',
  ADD COLUMN IF NOT EXISTS steam_appid INT NULL COMMENT 'Steam AppId',
  ADD COLUMN IF NOT EXISTS rawg_updated_at DATETIME NULL COMMENT 'RAWG updated';

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_external
  ON game (external_provider, external_id, is_delete);

CREATE INDEX IF NOT EXISTS idx_game_name_zh ON game(name_zh);

CREATE TABLE IF NOT EXISTS game_alias (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  locale VARCHAR(8) NOT NULL DEFAULT 'zh',
  name VARCHAR(255) NOT NULL,
  source VARCHAR(32) NOT NULL COMMENT 'douban/user/rawg',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_alias_name (name),
  INDEX idx_alias_game (game_id)
) COMMENT '游戏搜索别名';

CREATE TABLE IF NOT EXISTS game_sync_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_name VARCHAR(64) NOT NULL,
  last_page INT DEFAULT 0,
  total_imported INT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'idle' COMMENT 'idle/running/done/failed',
  error_message VARCHAR(1000) NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_job (job_name)
) COMMENT 'RAWG 同步断点';
