# GRecord

> **ゲーム記録** — 记录游戏时光，打卡精彩瞬间。

游戏游玩记录与打卡平台：管理游戏库、每日打卡、游玩热力图与想玩清单。前后端分离，Kawaii 暗色主题界面。

---

## 项目简介

| 项 | 说明 |
|---|---|
| **定位** | 个人/小团队游戏记录与打卡 |
| **前端** | Vue 3 · TypeScript · Vite · Pinia · Vue Router |
| **后端** | Java 17 · Spring Boot 3 · MyBatis-Plus · JWT |
| **数据** | MySQL 8 |
| **仓库** | [github.com/kiki3231/Grecord](https://github.com/kiki3231/Grecord) |

**GitHub About 建议填写（仓库 Settings → About）：**

- **Description:** `游戏记录打卡网站 · Vue3 + Spring Boot · 热力图 / 想玩清单 / 头像`
- **Topics:** `vue3`, `spring-boot`, `game-tracking`, `check-in`, `typescript`, `mybatis-plus`

---

## 功能特性

- 用户注册 / 登录（JWT）、头像上传
- 游戏库管理、游玩记录与打卡
- 仪表盘：游玩热力图、统计概览、今日任务
- 想玩清单（Backlog）与评分
- 响应式布局，侧栏折叠与多尺寸适配

---

## 品牌资源

Logo 为 **v3-03 线稿游戏手柄**，源文件与 favicon 位于：

```
frontend/public/brand/grecord-pad-outline.svg
frontend/public/favicon.ico
frontend/src/components/brand/BrandLogo.vue
```

---

## 环境要求

- JDK 17+、Maven 3.8+
- Node.js 18+、npm 9+
- MySQL 8

---

## 快速开始

### 1. 数据库

创建数据库并导入项目 SQL（见 `backend` 或文档中的建表脚本）。

### 2. 后端

```bash
cd backend
# 复制并编辑本地配置（勿提交密钥）
# src/main/resources/application-local.yml
mvn spring-boot:run
```

默认端口见 `backend/src/main/resources/application.yml`。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认 `http://localhost:5173`，API 代理请在 `vite.config.ts` 中配置。

### 4. 生产构建

```bash
cd frontend && npm run build
cd backend && mvn -DskipTests package
```

---

## 目录结构

```
GRecord/
├── backend/          # Spring Boot API
├── frontend/         # Vue 3 SPA
├── data/avatar/      # 用户头像（运行时，已 gitignore）
├── docs/plans/       # 设计备忘
└── readme.md
```

---

## 开发说明

- 敏感配置使用 `application-local.yml` / 环境变量，**不要**提交密码与密钥。
- 用户头像写入 `data/avatar/`，已在 `.gitignore` 中排除。
- 提交信息建议遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)，示例：`feat(dashboard): 优化热力图配色`。

---

## License

私有学习/个人项目；如需开源请自行补充 License 文件。
