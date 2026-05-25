# 游戏目录数据源与离线存储 设计说明

> 状态：已确认（2026-05-25）  
> 范围：游戏库公共目录（方案 A）+ 桌面 exe 离线架构（架构 B）

## 背景与目标

### 要解决的问题

- 现有豆瓣爬虫仅约 **900** 条，无法支撑「游戏库」规模，且数据源不稳定、难以增量更新。
- 产品初心是最终发布 **`.exe` 桌面应用** 给大家使用，用户机器上不能依赖 MySQL；需支持 **离线使用**。
- 目标优先级：**数据尽量全（B）**、**中文体验（D）**、**免费（C）**；预置规模选 **约 1 万款（A）**。

### 成功标准

- 用户 **无网络** 时可：浏览约 1 万款游戏、筛选、本地搜索、想玩/打卡等个人功能。
- 用户 **有网络** 时可：更新游戏目录包、（可选）在线补搜未收录游戏；封面可分包下载或懒缓存。
- 游戏目录由 **RAWG** 等在 **构建机/服务端** 同步，API Key **不进入 exe**。
- 合规：应用内 **署名 RAWG** 并链至官网；不把 API 数据转售或开放给第三方。

---

## 现状与约束

### 现有实现

- 数据流：豆瓣 Scrapy → `游戏数据.txt` → Python 导入 → MySQL `game`（`source=douban`）。
- 后端：`GET /game/search`、`/hot`、`/filters`，纯 MySQL `LIKE` 查询。
- 前端：Library 分页浏览与搜索；`POST /game` 支持 `source=user`。
- `game.source` 字段已预留 `douban/igdb/user`，迁移脚本提及 `rawg`。

### 约束

| 约束 | 说明 |
|------|------|
| RAWG 免费档 | 约 **20,000 请求/月**；个人/hobby；页面需 **RAWG 署名链接** |
| 不可客户端直连 RAWG | exe 分发会导致 API Key 泄露 |
| 离线 | 浏览/筛选/本地搜索/个人数据不依赖网络 |
| 安装包体积 | 元数据可内置；封面宜分包或懒加载，避免数 GB 安装包 |
| 豆瓣 900 条 | 仅作合并/别名参考，**不作为扩库来源** |

---

## 方案对比

### 方案一：网站 + MySQL 中心化（现状延伸）

- 优点：与当前代码一致，实现快。
- 缺点：**不适合 exe 离线**；每台用户机无 MySQL。

### 方案二：瘦客户端 exe + 云端目录（架构 I）

- 优点：RAWG 回源自然；目录始终最新。
- 缺点：**离线不可用**；需长期 hosting。

### 方案三：厚客户端离线 + 目录包（架构 B，推荐）

- 优点：符合「给大家用的软件」；离线体验完整；RAWG 仅在构建时消耗配额。
- 缺点：需 **catalog 构建/发布流水线**；目录更新需版本机制。

### 数据源方案（目录内容）

| 方案 | 说明 | 结论 |
|------|------|------|
| 继续豆瓣爬虫 | 不稳定、难破 900+ | ❌ 放弃 |
| Wikidata _bulk | 规模大、字段稀疏 | ❌ 不作主库 |
| RAWG 主源 + Steam/Wikidata 中文 | 免费、可预置 1 万 + 构建时 enrichment | ✅ 采用（方案 A 内容层） |

---

## 推荐方案

**目录内容：方案 A（RAWG 预置约 1 万 + 构建时中文 enrichment + 可选在线补搜）**  
**交付形态：架构 B（离线 `catalog.db` + 本地 `user.db`）**

开发期可继续使用 **MySQL** 验证导入与字段；发布 exe 前 **导出** 为 `catalog.db`。MySQL 是构建/网站阶段的工具库，**不是**用户机器上的终点存储。

---

## 详细设计

### 架构

```text
[构建机 / CI]
  RAWG 分页 (~10k) → 规范化 → (MySQL 开发库) → 导出 catalog.db
  Steam schinese / Wikidata zh / 豆瓣 alias 合并
  封面 → manifest + 可选 thumb 包
       ↓
  发布 catalog-{version}.zip

[用户 PC]
  GRecord.exe
  ├── catalog/catalog.db      (只读, ~10k)
  ├── catalog/catalog.version
  ├── user.db                 (打卡、想玩、用户添加游戏)
  └── cache/covers/

联网可选:
  检查 catalog 更新 → 下载替换 catalog/
  在线补搜 → 仅经自有服务端 RAWG → 写入 user 侧扩展，不篡改只读 catalog
```

### 本机目录布局

```text
%AppData%/GRecord/
├── user.db
├── catalog/
│   ├── catalog.db
│   ├── catalog.version      # 例: 2026.05.1
│   └── manifest.json        # 封面包校验、URL、大小
├── cache/covers/
└── logs/
```

安装包携带：`exe` + `catalog/catalog.db`（元数据目标 < 30MB）；封面可选内置 Top N 缩略图或首次联网下载。

### 关键组件

| 组件 | 职责 |
|------|------|
| **CatalogSyncJob**（构建机） | RAWG 分页拉取、断点续传、去重、`sync_log` |
| **CatalogEnricher** | Steam `appdetails`（l=schinese）、Wikidata `label@zh`、`game_alias` |
| **CatalogExporter** | MySQL / 中间格式 → `catalog.db` + `version` + `manifest.json` |
| **CatalogUpdateChecker**（exe） | 联网检查新版本、校验 SHA256、替换 catalog |
| **LocalGameRepository**（exe） | 对 `catalog.db` 只读 SQL；对 `user.db` 读写 |
| **OnlineSearchRelay**（可选，服务端） | 代理 RAWG search；API Key 仅服务端 |
| **CoverCache** | 按 manifest 或列表懒加载至 `cache/covers/` |

### 数据模型要点

**catalog.db（只读，与现 `game` 对齐并扩展）**

| 字段/表 | 说明 |
|---------|------|
| `game` | id, name, name_zh, description, icon_path, platforms, game_types, rating, developer, source, status… |
| `external_provider` + `external_id` | 如 `rawg` + `3498`；**稳定键**，版本更新时映射 game_id |
| `game_alias` | locale, name, source（豆瓣名、用户别名） |
| `synced_at` | 构建时间 |

**user.db（读写）**

| 内容 | 说明 |
|------|------|
| `game_record` / `game_backlog` | 引用 catalog `game_id` 或 **external_id**（推荐存 external_id 防 catalog 换版断链） |
| `user_game` | `source=user` 或在线缓存条目 |
| 设置、本地 JWT（若需要） | 按产品定 |

**豆瓣 900 条**：一次性合并；高置信 `external_id` 合并，其余保留 alias 或 `source=douban` 待审。

### 数据流

#### 构建流（方案 A）

1. RAWG `GET /games?ordering=-rating`，`page_size=40`，约 250 页 ≈ 1 万条（**~250 次请求**，列表字段足够则不打 detail）。
2. 规范化平台/类型（维护 **RAWG → 展示中文** 映射表）。
3. 分批 Steam / Wikidata 补 `name_zh`（3～4 周，限速 1～2 req/s）。
4. 封面下载至构建产物；生成 `manifest.json`。
5. 导出 `catalog.db`，写入 `catalog.version`。

#### 运行时（exe 离线）

1. Library 浏览 / 筛选 / 排序 → 只查 `catalog.db`。
2. 搜索 → `name` / `name_zh` / `game_alias` + `user.db` 用户游戏。
3. 无结果 → 提示手动添加；联网时可选「在线查找」。

#### 运行时（联网）

1. 启动或设置 → `GET /catalog/latest.json` → 下载 zip → 校验 → 替换 `catalog/` → **external_id 映射** 修复用户引用。
2. 在线补搜（可选）→ 服务端 RAWG → 返回条目写入 `user.db`，**不修改** 只读 catalog。

### 配额规划（RAWG，构建机）

| 用途 | 约请求数/月 |
|------|-------------|
| 首月预置 1 万（列表） | ~250～2250 |
| Steam 中文 enrichment | ~3000（分周） |
| Wikidata 批量 | ~500 |
| 余量 | 留给增量 rebuild、在线中继（若网站版同步开启） |

搜索懒加载 **不在用户 exe 直连**；若提供在线补搜，消耗 **服务端** 配额，需节流与缓存。

### 离线 / 联网能力矩阵

| 功能 | 离线 | 联网 |
|------|------|------|
| 浏览、筛选、热门排序 | ✅ | ✅ |
| 关键词搜索（catalog + user） | ✅ | ✅ |
| 想玩 / 打卡 / 统计 | ✅ | ✅ |
| 更新 catalog 包 | ❌ | ✅ |
| 下载封面包 / 懒缓存封面 | ❌ | ✅ |
| RAWG 在线补搜 | ❌ | 可选 ✅ |
| 手动添加游戏 | ✅ | ✅ |

### 异常与边界处理

| 场景 | 处理 |
|------|------|
| RAWG 构建配额用尽 | 断点续传下月继续；已导出 catalog 版本仍可用 |
| Steam 限速 | 退避、分周；无中文则显示英文名 |
| catalog 更新后 id 变化 | **以 external_id 为用户数据外键**；发布时带 id 映射表 |
| 搜索空结果 | 离线：手动添加；在线：可选中继 |
| 封面缺失 | 占位图 + 缓存目录 |
| 平台/类型中英文混杂 | 入库映射表统一展示语言 |
| 合规 | 关于页 RAWG 链接；规模商业化时升级 RAWG Business |

### 测试策略

- **构建**：重复 upsert 不 duplicated；断点续传；导出 catalog 行数 ≈ 1 万。
- **catalog 质量**：字段覆盖率；Steam 中文命中率抽样；中文关键词搜索命中率抽样。
- **exe 离线**：无网启动；浏览/搜/打卡；user.db 持久化。
- **更新**：版本替换后 backlog/record 仍指向正确游戏（external_id）。
- **合规**：署名页存在；安装包无 API Key。

### 与现有 Web 的演进

| 阶段 | 内容 |
|------|------|
| 1 | MySQL + RAWG 同步管道；字段与映射跑通 |
| 2 | CatalogExporter → `catalog.db`；Web 仍用 MySQL 或读导出文件验证 |
| 3 | exe：Tauri/Electron + 本地 SQLite；复用 Vue UI |
| 4 | catalog 更新检查 + 封面包 |
| 5 | 可选：在线补搜中继、账号云同步 |

---

## 风险与待确认项

| 风险 | 级别 | 备注 |
|------|------|------|
| 中文覆盖率低于预期 | 中 | 接受热门优先；别名与用户添加补长尾 |
| 安装包/封面包体积 | 中 | 元数据内置，封面分包 |
| RAWG 免费政策 / 商用 | 低～中 | 公开发布 exe 后若商业化需 Business |
| 双端维护 | 中 | 共用同一 catalog 构建产物 |
| Web 与 exe 账号是否互通 | 待产品定 | 不影响 catalog 设计 |

---

## 附录：已确认的决策记录

- 数据源：**RAWG 主源**，停用豆瓣爬虫扩库。
- 预置规模：**约 1 万**（分批 2～4 周构建）。
- 中文：Steam schinese → Wikidata → alias / 用户补全。
- 交付：**离线架构 B**（`catalog.db` + `user.db`）。
- MySQL：**开发/构建期**，非 exe 终点。
- 豆瓣 900：**合并与别名**，非规模基础。
