# CLAUDE.md

本文件为 Claude Code 提供项目指引，帮助其理解代码结构、规范和约定。

## 项目概述

校园失物招领系统 — 用户可发布失物/招领信息、浏览搜索物品、提交认领申请并完成审核流程。管理员通过后台管理用户、物品、分类和系统设置。

- **用户角色**：普通用户（学生/教职工）和管理员
- **Gitee 仓库**：https://gitee.com/NovaForge/lost-found-system

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.2.0, MyBatis Plus 3.5.7 |
| 前端 | Vue 3.2 + Vite 4.5, Element Plus 2.9, Pinia 3.0, Vue Router 4 |
| 数据库 | MySQL 8.0（JDBC 连接，库名 `lost_found_db`） |
| 认证 | Spring Security + JWT（java-jwt 4.4.0, BCrypt） |
| 接口文档 | Knife4j（OpenAPI 3）— `/swagger-ui.html` |
| 工具库 | Hutool 5.8.25, Lombok |
| 构建 | Maven（后端）, Vite（前端） |

## 项目结构

```
├── springboot/                     # Spring Boot 后端（端口 1235）
│   └── src/main/java/org/example/springboot/
│       ├── SpringbootApplication.java   # 启动入口，@EnableScheduling
│       ├── common/                 # Result<T> 统一返回、ResultCode 状态码
│       ├── config/                 # SecurityConfig、JwtAuthenticationFilter、
│       │                           #   MybatisPlusConfig、Knife4jConfig、
│       │                           #   WebConfig、AsyncConfig、FilePathConfig
│       ├── controller/             # REST 控制器
│       ├── DTO/                    # 请求/响应 DTO
│       ├── entity/                 # 实体类（User, LostItem, FoundItem 继承
│       │                           #   BaseItem, ClaimApplication 等）
│       ├── enumClass/              # 枚举（ItemStatus, AccountStatus,
│       │                           #   FileType, PointsChangeType）
│       ├── exception/              # ServiceException, GlobalExceptionHandler
│       ├── mapper/                 # MyBatis Plus Mapper 接口
│       ├── service/                # 业务逻辑层
│       ├── task/                   # 定时任务（ItemStatusTask）
│       └── util/                   # JwtTokenUtils, JwtAuthUtils, FileUtil,
│                                   #   DateUtils, ValidationUtils, ItemFillHelper
├── vue3/                           # Vue 3 前端（开发端口 3002）
│   └── src/
│       ├── api/                    # API 调用模块
│       ├── components/             # 共享组件（ItemList, AdvancedSearch,
│       │   ├── backend/            #   ConfirmDialog 等）
│       │   ├── common/
│       │   └── frontend/
│       ├── layouts/                # FrontendLayout（前台）, BackendLayout（后台）
│       ├── router/                 # Vue Router 配置 + 路由守卫
│       ├── store/                  # Pinia 状态管理（user, app）
│       ├── utils/                  # request.js（axios封装）、statusUtils.js、
│       │                           #   dateUtils.js、imageUtils.js
│       └── views/                  # 页面组件
│           ├── auth/               # 登录、注册
│           ├── backend/            # 控制台、用户/分类/失物/招领/
│           │                       #   认领/系统设置/会员管理
│           ├── frontend/           # 首页、失物/招领列表与详情、
│           │                       #   发布/编辑、认领、通知
│           └── profile/            # 个人中心
├── lost_found_db.sql               # 完整数据库导出
├── 项目打包/                        # 部署打包文件
└── PROJECT_INFO.md                 # 辅助信息（Gitee、DB 等）
```

## 常用命令

### 后端

```bash
# 启动后端（端口 1235）
cd springboot
mvn spring-boot:run

# 也可直接在 IDE 中运行 SpringbootApplication.java
# 注意：IDE 工作目录必须设置为 springboot/ 模块目录

# 打包（跳过测试）
mvn clean package -DskipTests

# 运行测试
mvn test
```

### 前端

```bash
# 启动开发服务器（端口 3002，/api 代理到 localhost:1235）
cd vue3
npm run dev

# 生产构建
npm run build
```

### 数据库

```bash
# 本地连接
mysql -u root -p123456 --ssl-mode=DISABLED lost_found_db
```

### 本地访问地址

| URL | 说明 | 认证 |
|-----|------|------|
| http://localhost:1235/swagger-ui.html | API 文档（Knife4j） | admin/admin |
| http://localhost:3002 | 前端开发服务器 | — |
| http://localhost:1235 | 后端 API | — |

## 架构与规范

### 后端分层

```
Controller → Service → Mapper → DB
     ↓
  Result<T>（统一返回包装）
```

- **Controller**：只处理 HTTP 相关 — 提取参数、调用 Service、用 `Result<T>` 包装返回。使用 `@Tag` 注解（Knife4j 分组）。
- **Service**：业务逻辑与事务管理。业务错误抛出 `ServiceException`。
- **Mapper**：继承 MyBatis Plus 的 `BaseMapper<T>`。复杂 SQL 写在 `resources/mapper/*.xml`。
- **Entity**：POJO，使用 `@Data`（Lombok）、`@TableName`、`@TableId(type = IdType.AUTO)`。非持久化展示字段用 `@TableField(exist = false)`。

### API 设计

- **返回格式**：`Result<T>` — `{ code: "200", msg: "...", data: ... }`
- **成功状态码**：`"200"`（String 类型，不是 Integer）
- **URL 规范**：`/api/{资源名}`（如 `/api/user/login`、`/api/lost-item/page`）
- **异常处理**：`GlobalExceptionHandler` 统一捕获 — `ServiceException` → 业务错误，`Exception` → `"500"` 系统错误
- **分页**：MyBatis Plus `Page<T>`，参数名 `currentPage`（从1开始）、`size`

### 安全机制

- **认证流程**：JWT 方式。登录返回 token，前端存 `localStorage`，请求时通过 `token` 请求头携带（**不是** `Authorization: Bearer ...`）。
- **过滤器**：`JwtAuthenticationFilter` 在 `UsernamePasswordAuthenticationFilter` 之前执行，从 header 提取 token，设置 SecurityContext。
- **公开接口**：在 `SecurityConfig` 中白名单放行 — 登录、注册、物品浏览、分类、验证码、文件访问。
- **管理员接口**：`/api/admin/**`、`/api/back/**` 需要 `ROLE_ADMIN`。
- **密码编码**：`BCryptPasswordEncoder(10)` — Bean 名称为 `passwordEncoder()`。
- **敏感配置**：`JWT_SECRET`、`DB_PASSWORD` 等均支持环境变量覆盖（`${ENV:默认值}` 语法）。

### 物品状态（5 状态模型）

```
0 待认领 → 1 已认领 → 2 已交接 → 3 已关闭
         ↘ 3 已关闭
         ↘ 4 已过期 → 待认领（重新开放）/ 已关闭
```

- `ItemStatus` 枚举通过 `canTransitionTo()` 校验合法状态转换。
- `LostItem` 和 `FoundItem` 均继承 `BaseItem` 抽象类，共享 `status` 和 `isPinned` 字段。
- 置顶物品（`is_pinned = 1`）会被定时任务跳过，不会自动过期。

### 前端约定

- **HTTP 请求**：`utils/request.js` 封装 axios — JWT 通过 `config.headers['token']` 发送，成功时自动解包到 `res.data`，失败时 ElMessage 提示。
- **状态管理**：Pinia（`store/user.js` 中的 `useUserStore`）。
- **路由布局**：两套布局 — `FrontendLayout`（`/`，前台）和 `BackendLayout`（`/back`，后台）。路由守卫检查 `requiresAuth` meta，非管理员会被重定向离开 `/back`。
- **登录/注册页**：`/login`、`/register`，独立于布局之外。
- **组件命名**：文件用 PascalCase，模板中用 kebab-case。

## 开发模式

### 新增功能（以新实体为例）

1. **Entity** — 创建 POJO，继承 `BaseItem` 或独立，添加 `@Data`、`@TableName`、`@TableId`
2. **Mapper** — 创建接口继承 `BaseMapper<Entity>`
3. **Service** — 业务逻辑，`@Service`，注入 Mapper
4. **Controller** — `@RestController`、`@RequestMapping`，注入 Service，用 `Result` 包装返回值
5. **前端** — 添加 API 函数、页面组件、路由

### 定时任务

- `ItemStatusTask` 每天凌晨 2 点执行 — 自动将超过过期天数的物品标记为过期（过期天数可通过 `system_config` 表配置，默认 30 天）。
- 使用 `@Scheduled` 注解，启动类需有 `@EnableScheduling`。

### 文件上传

- 文件存储在 `file.base-path` 配置的目录下（默认 `./files`）。
- `FileService` 处理存储，`FileController` 通过 `/file/**` 和 `/api/file/**` 提供文件访问。
- 图片以逗号分隔的路径字符串存储在实体的 `images` 字段中。

### 系统配置驱动

- 验证码开关：`system_config` 表 key `captcha.enabled` → `"true"` / `"false"`
- 物品过期天数：`system_config` 表 key `item.expire.days`
- `SystemConfigService.getConfigValue(key)` 读取配置（带缓存）

## 注意事项（踩坑指南）

- ⚠️ **IDE 工作目录**：在 IDE 中运行 `SpringbootApplication.java` 时，工作目录必须设为 `springboot/` 模块目录，否则 `file.base-path` 路径解析错误。
- ⚠️ **JWT 请求头名称**：是 `token`，不是 `Authorization: Bearer ...`，写错会导致 401。
- ⚠️ **密码编码器 Bean 名**：是 `passwordEncoder()`，BCrypt 强度 10。如需按名称注入，注意使用 `@Qualifier`。
- ⚠️ **Result 状态码**：是 `String` 类型，不是 `Integer`。`ResultCode.SUCCESS.getCode()` 返回 `"200"`。
- ⚠️ **MyBatis Plus**：版本 `3.5.7`，使用 `spring-boot3-starter` 变体（兼容 Spring Boot 3.x / Jakarta EE）。
- ⚠️ **置顶与过期**：`is_pinned = 1` 的物品永不过期。管理员设置置顶时请确认该字段确实被写入。
- ⚠️ **前端 store 导入路径**：正确路径是 `@/store/user`（单数 `store`），不是 `@/stores/user`。部分旧代码可能引用错误，使用时务必核对。
- ⚠️ **ElMessage 导入**：在 `<script setup>` 中必须显式导入 `ElMessage`。部分旧组件可能遗漏此导入。
- ⚠️ **数据库迁移**：`db/migration/` 下的 SQL 文件（`V2__add_pin_and_system_config.sql`、`V3__add_membership.sql`）需手动执行，因为 pom.xml 中未配置 Flyway 依赖。
- ⚠️ **前端代理**：Vite 将 `/api` 代理到 `localhost:1235`。启动前端前请确保后端已运行。

## 数据库信息

| 配置项 | 值 |
|--------|-----|
| 地址 | `localhost:3306` |
| 数据库 | `lost_found_db` |
| 用户名 | `root` |
| 密码 | `123456`（仅本地开发） |
| SSL | 关闭 |

主要表：`user`、`lost_item`、`found_item`、`item_category`、`claim_application`、`notification`、`system_config`、`membership_record`、`points_log`。

## 环境变量

| 变量名 | 用途 | 开发环境默认值 |
|--------|------|---------------|
| `DB_PASSWORD` | MySQL 密码 | `123456` |
| `JWT_SECRET` | JWT 签名密钥 | `dev-secret-do-not-use-in-production` |
| `SPRING_SECURITY_PASSWORD` | Swagger 基本认证 | `admin` |
| `KNIFE4J_PASSWORD` | Knife4j 基本认证 | `admin` |
| `USER_DEFAULT_PASSWORD` | 新用户默认密码 | `123456` |

**生产部署**：务必通过环境变量设置以上全部配置，并删除 `application.properties` 中的 `:默认值` 后缀。
