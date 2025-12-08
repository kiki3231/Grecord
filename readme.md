### GRecord — 全栈项目 README（从0到上线）

一个人完成前后端游戏打卡网站的参考方案与落地指南。包含技术选型、目录结构、开发流程、联调规范、以及云服务器部署与运维建议。

### 目标与范围

- 最小可行：注册/登录（JWT）、游戏管理、打卡记录、统计概览。

- 能跑能迭代：本地开发流畅，接口清晰，日志与错误可追踪。

- 可上线：一键构建，Docker 化部署，HTTPS，监控与备份。

### 技术栈（常用且稳定）

- 前端

- 框架：Vue 3 + TypeScript

- 构建：Vite、@vitejs/plugin-vue

- 状态：Pinia（可后加）

- 路由：Vue Router 4

- 网络：Axios

- UI：自写样式/Tailwind/任一 UI 库（Element Plus/Ant Design Vue）

- 后端

- 语言/框架：Java 17、Spring Boot 3

- Web & 校验：Spring Web、Jakarta Validation

- 数据访问：MyBatis-Plus

- 鉴权：JWT（Filter/Interceptor）、Spring Security

- 文档：OpenAPI/Swagger（springdoc-openapi）

- 日志：Logback

- 数据库与基础设施

- MySQL 8、

- Redis（可选：会话/缓存/限流）

- Docker + Docker Compose

- 反代与静态：Nginx

- 域名与证书：Let’s Encrypt（certbot）

- 质量保障

- 前端：ESLint、Prettier、Stylelint（可选）

- 后端：Spotless/Checkstyle、JUnit、Mock

- CI/CD：GitHub Actions（或 Gitee）

  

  ### 环境要求

  - Windows（PowerShell）或 macOS/Linux

  - JDK 17、Maven 3.8+

  - Node.js 18+、npm 9+

  - Docker 24+、Docker Compose v2+

  - MySQL 8（本地或容器）