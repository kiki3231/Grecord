# RAWG + MySQL + Redis 游戏数据源 实施计划

> **给 Claude：** 必须使用 `executing-plans` 子技能，按任务逐项执行本计划。

**目标：** 用 RAWG API 替代豆瓣爬虫，MySQL 作为游戏目录主存储，Redis 作为热点/API 结果缓存；支持约 1 万预置 + 搜索懒加载，避免与旧数据重复。

**架构方案：** 服务端 `RawgClient` 拉取数据 → `GameSyncService` 规范化 upsert 至 MySQL（`external_provider` + `external_id` 唯一）→ 现有 `GameController` 读库；搜索本地不足时回源 RAWG 再入库。Redis 缓存 filters/hot/search 结果与同步锁，不存主数据。

**技术栈：** Spring Boot 3.2、MyBatis-Plus、MySQL 8、Redis（已配置 `RedisConfig`）、RestClient/WebClient、JUnit 5

**设计参考：** `@docs/plans/2026-05-25-game-catalog-offline-design.md`

---

## 总体分步路线图

| 阶段 | 名称 | 交付物 | 可独立验收 |
|------|------|--------|------------|
| **0** | 准备与旧数据策略 | RAWG Key、备份、清库/迁移脚本 | ✅ |
| **1** | MySQL 表结构重构 | migration SQL + Entity 更新 | ✅ |
| **2** | RAWG 客户端 + 同步核心 | RawgClient、DTO、GameSyncService | ✅ |
| **3** | 批量导入 ~1 万 | 定时/CLI 任务、sync_log | ✅ |
| **4** | 搜索回源 + 业务 API | searchGames 增强、GameService 测试 | ✅ |
| **5** | Redis 加速层 | GameCacheService、限流/锁 | ✅ |
| **6** | 中文 + 封面 | name_zh、alias、本地下载 | ✅ |
| **7** | 前端与收尾 | RAWG 署名、停用 Scrapy | ✅ |

---

## 架构与数据流

```text
                    ┌─────────────────┐
  阶段3 定时/CLI     │  RawgClient     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ GameSyncService │─── Redis: sync lock / quota counter
                    │ upsert by       │
                    │ external_id     │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
  阶段4/5           │  MySQL game     │◄─── game_record / game_backlog (FK)
                    │  + game_alias   │
                    └────────┬────────┘
                             │
  GET /game/*       ┌────────▼────────┐
                    │ GameService     │─── Redis: hot / filters / search cache
                    └─────────────────┘
```

### Redis Key 约定

| Key | 类型 | TTL | 用途 |
|-----|------|-----|------|
| `grecord:game:filters` | String(JSON) | 1h | `/game/filters` 聚合结果 |
| `grecord:game:hot:{page}:{size}` | String(JSON) | 10m | 热门列表 |
| `grecord:game:search:{md5(params)}` | String(JSON) | 5m | 纯本地搜索结果 |
| `grecord:rawg:search:{md5(keyword)}` | String(JSON) | 24h | RAWG 回源结果，省配额 |
| `grecord:sync:rawg:lock` | String | 30m | 同步任务分布式锁 |
| `grecord:rawg:requests:{yyyyMM}` | String(counter) | 35d | 当月 API 调用计数（可选） |

Redis 不可用时的策略：**与 `GameRecordServiceImpl` 热力图一致，catch 后降级直查 MySQL**，不影响核心功能。

---

## 阶段 0：准备与旧数据策略

### 0.1 注册 RAWG API Key

1. 访问 https://rawg.io/apidocs 注册并获取 Key。
2. 本地创建 `backend/.env.local`（不入库）或环境变量：

```bash
RAWG_API_KEY=your_key_here
```

3. 验证：

```bash
curl "https://api.rawg.io/api/games?page_size=1&key=YOUR_KEY"
```

预期：返回 JSON，`count` > 0。

### 0.2 数据库备份

```bash
mysqldump -u root -p grecord > backup/grecord_before_rawg_$(date +%Y%m%d).sql
```

### 0.3 旧数据清理策略（三选一，推荐 B）

**A. 开发环境清空重来（最简单）**

- 若无重要用户数据：`DELETE FROM game WHERE source = 'douban'` 或 `TRUNCATE` 相关（注意 FK）。
- `game_record` / `game_backlog` 若引用 douban `game_id`，需一并清理或置空。

**B. 保留用户关联，删除无引用 douban（推荐）**

```sql
-- 1) 找出仍被用户使用的 game_id
CREATE TEMPORARY TABLE tmp_used_game AS
SELECT DISTINCT game_id FROM game_record WHERE is_delete = 0
UNION
SELECT DISTINCT game_id FROM game_backlog WHERE is_delete = 0;

-- 2) 删除未被引用的 douban 游戏
DELETE FROM game
WHERE source = 'douban'
  AND id NOT IN (SELECT game_id FROM tmp_used_game);

-- 3) 仍被引用的 douban 行：保留至阶段6合并，或手动处理
```

**C. 软删除全部 douban，RAWG 全新入库**

```sql
UPDATE game SET is_delete = 1 WHERE source = 'douban';
```

- 用户旧 `game_id` 仍有效，Library 默认不展示 `is_delete=1`。
- 新 RAWG 数据 `source='rawg'`，无 id 冲突；后续用 alias 合并展示。

**豆瓣别名保留（阶段6用）：**

```sql
-- 导出备用
SELECT id, name, rating, review INTO OUTFILE '/tmp/douban_games.csv'
FROM game WHERE source = 'douban';
```

或在 Java 迁移任务中读表写入 `game_alias` 后再删。

---

## 阶段 1：MySQL 表结构重构

### 任务 1.1：编写 migration SQL

**涉及文件：**
- 新建：`backend/src/main/resources/sql/V2_rawg_catalog.sql`

**内容要点：**

```sql
-- game 表扩展
ALTER TABLE game
  ADD COLUMN IF NOT EXISTS external_provider VARCHAR(16) NULL COMMENT 'rawg/steam/user',
  ADD COLUMN IF NOT EXISTS external_id VARCHAR(64) NULL COMMENT '外部主键',
  ADD COLUMN IF NOT EXISTS name_zh VARCHAR(255) NULL COMMENT '中文名',
  ADD COLUMN IF NOT EXISTS steam_appid INT NULL COMMENT 'Steam AppId',
  ADD COLUMN IF NOT EXISTS rawg_updated_at DATETIME NULL COMMENT 'RAWG updated 字段';

-- 唯一约束：同一来源不重复
CREATE UNIQUE INDEX IF NOT EXISTS uk_game_external
  ON game (external_provider, external_id, is_delete);

CREATE INDEX IF NOT EXISTS idx_game_name_zh ON game(name_zh);

-- 别名表
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

-- 同步进度
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
```

**验证：** 在 MySQL 执行脚本，`\d game` 确认新列与索引存在。

### 任务 1.2：更新 Entity

**涉及文件：**
- 修改：`backend/src/main/java/com/ma/grecode/entity/Game.java`
- 新建：`backend/src/main/java/com/ma/grecode/entity/GameAlias.java`
- 新建：`backend/src/main/java/com/ma/grecode/entity/GameSyncLog.java`
- 新建：`backend/src/main/java/com/ma/grecode/mapper/GameAliasMapper.java`
- 新建：`backend/src/main/java/com/ma/grecode/mapper/GameSyncLogMapper.java`

**步骤：** 为 `Game` 增加 `externalProvider`, `externalId`, `nameZh`, `steamAppid`, `rawgUpdatedAt` 字段，与表列名对应。

**验证：** `mvn -f backend/pom.xml compile` 通过。

---

## 阶段 2：RAWG 客户端 + 同步核心

### 任务 2.1：配置项

**涉及文件：**
- 修改：`backend/src/main/resources/application.yml`

```yaml
rawg:
  api-key: ${RAWG_API_KEY:}
  base-url: https://api.rawg.io/api
  page-size: 40
  import-target: 10000
  ordering: -rating
  search-fallback-threshold: 5
  request-interval-ms: 300
```

- 新建：`backend/src/main/java/com/ma/grecode/config/RawgProperties.java`（`@ConfigurationProperties(prefix = "rawg")`）

### 任务 2.2：RAWG DTO 与 Client

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/integration/rawg/RawgGameListResponse.java`
- 新建：`backend/src/main/java/com/ma/grecode/integration/rawg/RawgGameDto.java`
- 新建：`backend/src/main/java/com/ma/grecode/integration/rawg/RawgClient.java`

**RawgClient 核心方法：**

```java
public RawgGameListResponse listGames(int page, String ordering);
public RawgGameDto getGame(long rawgId);
public RawgGameListResponse searchGames(String keyword, int page);
```

- 使用 Spring 6 `RestClient` 或 `WebClient`。
- 每次请求后 `Thread.sleep(requestIntervalMs)` 或使用 Resilience4j 限速（YAGNI：首版 sleep 即可）。
- Key 通过 query param `key=` 传递。

### 任务 2.3：字段映射器

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/integration/rawg/RawgGameMapper.java`
- 新建：`backend/src/main/resources/rawg/platform-mapping.properties`（可选，平台英文名 → 中文展示名）
- 新建：`backend/src/main/resources/rawg/genre-mapping.properties`

**映射规则：**

| RAWG | Game |
|------|------|
| `id` | `externalId`（String） |
| — | `externalProvider = "rawg"` |
| `name` | `name` |
| `description_raw` / `description` | `description` |
| `background_image` | `icon`（暂存 URL，阶段6下载） |
| `platforms[].platform.name` | `platforms`（逗号分隔，经 mapping） |
| `genres[].name` | `gameTypes` |
| `rating` | `rating` |
| `developers[0].name` | `developer` |
| `updated` | `rawgUpdatedAt` |
| stores 中 store slug=steam | `steamAppid` |
| — | `source = "rawg"`, `status = 0`, `isDelete = 0` |

### 任务 2.4：GameSyncService

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/service/GameSyncService.java`
- 新建：`backend/src/main/java/com/ma/grecode/service/impl/GameSyncServiceImpl.java`

**核心逻辑：**

```java
void upsertFromRawg(RawgGameDto dto);  // 按 external_provider+external_id 查重后 insert/update
int importPopularGames(int maxCount);  // 读 sync_log 断点，循环 listGames 直到 maxCount
int importSearchResults(String keyword, int maxPages);
```

**upsert 伪代码：**

```java
Game existing = gameMapper.selectOne(
  wrapper.eq(Game::getExternalProvider, "rawg")
         .eq(Game::getExternalId, String.valueOf(dto.getId()))
         .eq(Game::getIsDelete, 0));
Game mapped = rawgGameMapper.toEntity(dto);
if (existing != null) {
  mapped.setId(existing.getId());
  mapped.setCreateTime(existing.getCreateTime());
  updateById(mapped);
} else {
  save(mapped);
}
```

### 任务 2.5：单元测试（TDD）

**涉及文件：**
- 新建：`backend/src/test/java/com/ma/grecode/integration/rawg/RawgGameMapperTest.java`
- 新建：`backend/src/test/java/com/ma/grecode/service/GameSyncServiceTest.java`

- 用 `src/test/resources/rawg/sample-game.json` 固定样例测映射。
- `GameSyncServiceTest` 用 `@MockBean RawgClient` 测 upsert 不重复。

**验证：**

```bash
cd backend && mvn test -Dtest=RawgGameMapperTest,GameSyncServiceTest
```

预期：PASS。

### 任务 2.6：管理端触发接口（可选，便于 POC）

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/controller/GameSyncController.java`（`@PreAuthorize` 或仅 dev profile）

```java
@PostMapping("/admin/game/sync/import")
public AjaxResult<?> importBatch(@RequestParam(defaultValue = "200") int limit);
```

**验证：** Swagger 调用 `limit=10`，DB 中 `source=rawg` 增加 10 条，`external_id` 无重复。

---

## 阶段 3：批量导入约 1 万

### 任务 3.1：断点续传任务

**涉及文件：**
- 修改：`GameSyncServiceImpl.java`
- 修改：`game_sync_log` 读写

**逻辑：**

1. `job_name = 'rawg_popular_import'`
2. 从 `last_page + 1` 开始，`ordering=-rating`，每页 40 条
3. 每页 upsert 后更新 `last_page`、`total_imported`
4. `total_imported >= 10000` → `status=done`

**配额：** 约 250 次 list 请求；勿对每条再调 detail。

### 任务 3.2：CLI 或 Scheduled

**方案 A（推荐首版）：** 仅 Admin API 手动分批触发，避免误跑。

**方案 B：** `@Scheduled(cron = "0 0 3 * * ?")`  nightly 跑 500 条直到满 1 万。

**验证：**

```sql
SELECT COUNT(*) FROM game WHERE source='rawg' AND is_delete=0;
-- 预期逐步接近 10000

SELECT external_id, COUNT(*) c FROM game WHERE is_delete=0
GROUP BY external_provider, external_id HAVING c > 1;
-- 预期 0 行
```

---

## 阶段 4：搜索回源 + 业务 API

### 任务 4.1：增强 searchGames

**涉及文件：**
- 修改：`backend/src/main/java/com/ma/grecode/service/impl/GameServiceImpl.java`
- 修改：`backend/src/main/java/com/ma/grecode/service/GameService.java`
- 修改：`backend/src/main/resources/application.yml`（`search-fallback-threshold`）

**流程：**

```java
IPage<Game> local = searchLocal(keyword, platform, type, sortBy, page, size);
if (StringUtils.isBlank(keyword)) {
  return local;  // 无关键词不触发 RAWG
}
if (local.getTotal() >= threshold) {
  return local;
}
// 回源
gameSyncService.importSearchResults(keyword.trim(), 1);
return searchLocal(keyword, platform, type, sortBy, page, size);
```

### 任务 4.2：本地搜索含中文与别名

**修改 `searchLocal`：**

```java
wrapper.and(w -> w
  .like(Game::getName, kw)
  .or().like(Game::getNameZh, kw)
  .or().like(Game::getGameTypes, kw)
  // ...
);
// 子查询 game_alias.name LIKE kw → game_id IN (...)
```

### 任务 4.3：测试

**涉及文件：**
- 修改：`backend/src/test/java/com/ma/grecode/service/GameServiceTest.java`

- Mock `GameSyncService`，断言 local 不足时调用 `importSearchResults`。
- 断言有关键词且结果充足时不调 RAWG。

**验证：** `mvn test -Dtest=GameServiceTest`

### 任务 4.4：前端（小改）

**涉及文件：**
- 修改：`frontend/src/views/Library.vue`（页脚 RAWG 署名链接）
- 可选：`frontend/src/stores/game.ts` 搜索 loading 文案

```html
<p class="attribution">
  游戏数据由 <a href="https://rawg.io" target="_blank" rel="noopener">RAWG</a> 提供
</p>
```

---

## 阶段 5：Redis 加速层

### 任务 5.1：GameCacheService

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/service/GameCacheService.java`
- 新建：`backend/src/main/java/com/ma/grecode/service/impl/GameCacheServiceImpl.java`

**方法：**

```java
Optional<Map<String,Object>> getFilters();
void putFilters(Map<String,Object> data);
Optional<IPage<Game>> getHot(int page, int size);
void putHot(int page, int size, IPage<Game> data);
Optional<IPage<Game>> getSearch(String cacheKey);
void putSearch(String cacheKey, IPage<Game> data);
Optional<List<RawgGameDto>> getRawgSearch(String keyword);
void putRawgSearch(String keyword, List<RawgGameDto> data);
void evictAllGameCaches();  // 同步完成后调用
boolean trySyncLock();
void releaseSyncLock();
```

### 任务 5.2：接入 GameServiceImpl

- `getFilters()`：先 Redis，miss 则 DB 聚合后写入，TTL 1h。
- `getHotGames()`：先 Redis，TTL 10m。
- `searchGames()`：纯本地命中且 total>=threshold 时缓存 5m。
- `GameSyncService.importPopularGames` 开始/结束：`trySyncLock` / `releaseSyncLock`；完成后 `evictAllGameCaches()`。

### 任务 5.3：RAWG 搜索 dedup

在 `importSearchResults` 内：

```java
if (gameCacheService.getRawgSearch(keyword).isPresent()) {
  return 0; // 24h 内已拉过
}
// ... call rawg, upsert, putRawgSearch
```

### 任务 5.4：Redis 降级测试

- 停 Redis，`getHotGames` / `getFilters` 仍返回 DB 结果（与 GameRecordServiceImpl 同模式 try/catch）。

**验证：** 开 Redis 时第二次 `/game/filters` 响应更快；关 Redis 不 500。

---

## 阶段 6：中文 enrichment + 封面本地化

### 任务 6.1：Steam 简体中文（有 steam_appid）

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/integration/steam/SteamStoreClient.java`

```java
// GET https://store.steampowered.com/api/appdetails?appids={id}&l=schinese
// 解析 data.name, data.short_description → name_zh, description（可选覆盖策略：仅当 name_zh 空时写）
```

- 批量任务：扫描 `steam_appid IS NOT NULL AND name_zh IS NULL`，限速 1 req/s。

### 任务 6.2：豆瓣 alias 导入

- 对阶段0保留的 douban 行：插入 `game_alias(name=douban.name, source=douban)`。
- 若 RAWG 与 douban 名称模糊匹配成功：alias 挂到 RAWG 的 `game_id`，软删除 douban 行并 **迁移 FK**（若有引用）：

```sql
UPDATE game_record SET game_id = :newId WHERE game_id = :oldDoubanId;
UPDATE game_backlog SET game_id = :newId WHERE game_id = :oldDoubanId;
UPDATE game SET is_delete = 1 WHERE id = :oldDoubanId;
```

### 任务 6.3：封面下载

**涉及文件：**
- 新建：`backend/src/main/java/com/ma/grecode/service/GameCoverService.java`
- 配置：`game.cover.path: D:/GRecord/data/covers`（与 avatar 类似）
- `WebMvcConfig` 增加 `/covers/**` 静态映射

- 同步后异步下载 `background_image` → `{external_id}.jpg`，更新 `icon` 为 `/covers/rawg_{id}.jpg`。

---

## 阶段 7：收尾

### 任务 7.1：停用爬虫

- 在 `readme.md` 或 `游戏信息爬取/README` 标注 **deprecated**。
- CI/文档不再执行 Scrapy 导入。

### 任务 7.2：监控与运维

- 日志：`GameSyncService` 记录每页 imported count。
- 可选：`GET /admin/game/sync/status` 读 `game_sync_log`。
- 每月检查 RAWG 请求计数（Redis counter 或日志统计）。

### 任务 7.3：全链路验收清单

- [ ] `game` 表 `source=rawg` ≥ 10000（或达到目标后 done）
- [ ] `uk_game_external` 无重复
- [ ] Library 浏览、筛选、排序正常
- [ ] 搜冷门游戏名 → 回源 → 第二次本地命中
- [ ] Redis 关闭时服务正常
- [ ] Library 页 RAWG 链接存在
- [ ] 用户 backlog/record 未因迁移断链

---

## 风险与注意事项

| 风险 | 缓解 |
|------|------|
| 删除 douban 导致 FK 孤儿 | 阶段0选 B/C；合并脚本迁移 game_id |
| RAWG 配额用尽 | Redis 24h search dedup；sync 断点；Admin 手动控制 |
| `icon` 仍用外链 | 阶段6本地下载 |
| Redis 与 DB 不一致 | sync 完成 `evictAllGameCaches` |
| 平台/类型中英文混杂 | mapping.properties 统一展示 |

---

## 建议提交粒度（频繁 commit）

1. `feat(db): add rawg catalog schema and entities`
2. `feat(rawg): add client, mapper and sync service`
3. `feat(rawg): batch import with sync log`
4. `feat(game): search fallback to rawg`
5. `feat(cache): redis layer for game filters and hot`
6. `feat(i18n): steam zh and cover download`
7. `chore: deprecate douban scraper, add rawg attribution`

---

## 执行交接

**本计划已保存至 `docs/plans/2026-05-25-rawg-mysql-redis-implementation.md`。**

接下来有两种执行方式：

**1. 子代理驱动（当前会话内）** —— 在当前会话按任务分批实现，每批完成后审查。

**2. 并行会话（单独执行）** —— 在新会话中使用 `executing-plans` 按检查点批量执行（建议 git worktree）。

**你希望采用哪一种方式？**
