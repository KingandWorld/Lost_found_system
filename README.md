# 🎒 校园失物招领系统

> 一个基于 Spring Boot + Vue 3 的校园失物招领管理平台，帮助学生快速找回丢失物品或归还拾获物品。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-latest-409EFF.svg)](https://element-plus.org/)

---

## ✨ 功能特性

### 前台功能
- 🏠 **首页展示** — 最新失物信息、统计数据、使用指南
- 🔍 **失物浏览** — 按标题/分类搜索失物信息，卡片/列表双视图
- 📦 **招领浏览** — 浏览拾获物品信息
- 📝 **物品发布** — 发布失物/招领信息，支持多图上传
- 📋 **我的发布** — 管理自己发布的所有物品（含已过期）
- ✅ **认领申请** — 在线提交认领申请，查看审核进度
- 🔔 **消息通知** — 申请状态变更实时通知

### 后台管理
- 📊 **数据仪表盘** — 用户/物品/认领统计 + 状态分布图
- 👥 **用户管理** — 用户 CRUD、角色管理、状态启禁
- 📑 **失物管理** — 失物信息管理、状态变更、置顶操作
- 📦 **招领管理** — 招领信息管理、状态变更、置顶操作
- 📂 **分类管理** — 物品分类增删改查
- 📋 **认领审核** — 认领申请审批（通过/拒绝）
- ⚙️ **系统设置** — 可配置物品过期天数（1-365天）
- 📌 **物品置顶** — 管理员可置顶重要物品，永不过期

### 系统特性
- 🔐 **JWT 认证** + Spring Security 权限控制
- 🔒 **登录保护** — 5次失败锁定15分钟
- ⏰ **定时任务** — 每日凌晨自动处理过期物品
- 🖼️ **图片缩略图** — 上传自动生成，列表加载提速
- 📱 **响应式设计** — 适配桌面端和移动端
- 🎨 **设计系统** — 统一的 CSS 变量和组件库

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.x |
| **ORM** | MyBatis Plus | 3.x |
| **安全框架** | Spring Security + JWT | — |
| **数据库** | MySQL | 8.0 |
| **前端框架** | Vue 3 (Composition API) | 3.x |
| **UI 组件库** | Element Plus | latest |
| **状态管理** | Pinia | 2.x |
| **构建工具** | Vite | 5.x |
| **API 文档** | Swagger / Knife4j | — |

---

## 📁 项目结构

```
失物招领系统/
├── springboot/                    # 后端项目
│   └── src/main/java/org/example/springboot/
│       ├── config/                # 配置类（Security、Web、CORS）
│       ├── controller/            # 控制器层
│       │   ├── LostItemController.java
│       │   ├── FoundItemController.java
│       │   ├── UserController.java
│       │   ├── DashboardController.java
│       │   ├── FileController.java
│       │   └── ...
│       ├── service/               # 业务逻辑层
│       │   ├── LostItemService.java
│       │   ├── FoundItemService.java
│       │   ├── UserService.java
│       │   ├── ItemStatusService.java
│       │   └── ...
│       ├── entity/                # 实体类
│       ├── mapper/                # MyBatis Mapper
│       ├── util/                  # 工具类
│       └── enumClass/             # 枚举类
│
├── vue3/                          # 前端项目
│   └── src/
│       ├── views/
│       │   ├── frontend/          # 前台页面
│       │   │   ├── Home.vue       # 首页
│       │   │   ├── lost/          # 失物相关
│       │   │   ├── found/         # 招领相关
│       │   │   ├── my-items/      # 我的发布
│       │   │   ├── claim/         # 认领申请
│       │   │   └── notification/  # 消息通知
│       │   └── backend/           # 后台管理
│       │       ├── Dashboard.vue  # 数据仪表盘
│       │       ├── lost/          # 失物管理
│       │       ├── found/         # 招领管理
│       │       ├── user/          # 用户管理
│       │       ├── category/      # 分类管理
│       │       ├── claim/         # 认领审核
│       │       └── Settings.vue   # 系统设置
│       ├── components/            # 公共组件
│       │   ├── ItemList.vue       # 物品列表组件
│       │   └── common/            # 通用组件
│       │       ├── StatusTag.vue
│       │       └── ItemViewDialog.vue
│       ├── utils/                 # 工具函数
│       │   ├── statusUtils.js     # 状态工具
│       │   └── imageUtils.js      # 图片工具
│       ├── styles/                # 样式文件
│       │   ├── design-variables.css   # 设计变量
│       │   └── element-variables.scss # Element 主题
│       ├── store/                 # Pinia 状态管理
│       ├── router/                # 路由配置
│       └── layouts/               # 布局组件
│
└── PROJECT_INFO.md                # 项目辅助信息
```

---

## 🚀 快速开始

### 环境要求

- **JDK** 17+
- **MySQL** 8.0+
- **Node.js** 18+
- **Maven** 3.6+

### 1. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS lost_found_db DEFAULT CHARACTER SET utf8mb4;

-- 执行项目中的迁移脚本（如已初始化可跳过）
-- springboot/src/main/resources/db/migration/V2__add_pin_and_system_config.sql
```

### 2. 后端启动

```bash
cd springboot

# 修改 application.yml 中的数据库连接信息
# spring.datasource.url=jdbc:mysql://localhost:3306/lost_found_db
# spring.datasource.username=root
# spring.datasource.password=your_password

# 启动项目
mvn spring-boot:run
```

后端默认运行在 `http://localhost:1235`

API 文档：`http://localhost:1235/swagger-ui.html`（账号密码：admin/admin）

### 3. 前端启动

```bash
cd vue3

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`

---

## 📊 物品状态说明

| 状态码 | 状态名称 | 说明 |
|--------|----------|------|
| 0 | 待认领 | 物品发布后的初始状态 |
| 1 | 已认领 | 已被用户认领 |
| 2 | 已完成 | 认领流程完成，物品已交接 |
| 3 | 已关闭 | 物品信息被关闭 |
| 4 | 已过期 | 超过设定的过期天数（置顶物品永不过期） |

---

## 🔌 API 接口概览

### 失物管理 `/lost-item`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/page` | 分页查询（支持标题/分类/状态/用户筛选） |
| GET | `/{id}` | 查询详情 |
| POST | `/` | 发布失物信息 |
| PUT | `/{id}` | 修改失物信息 |
| DELETE | `/{id}` | 删除失物信息 |
| PUT | `/{id}/status` | 修改状态 |
| PUT | `/{id}/pin` | 置顶/取消置顶 |
| GET | `/statistics` | 获取统计信息 |

### 招领管理 `/found-item`
接口结构与失物管理一致。

### 用户管理 `/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/login` | 用户登录 |
| POST | `/add` | 创建用户 |
| GET | `/page` | 分页查询 |
| PUT | `/admin-reset-password/{id}` | 管理员重置密码 |
| POST | `/forget` | 忘记密码（需验证码） |

### 仪表盘 `/dashboard`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/statistics` | 获取完整后台统计数据 |

---

## 🗺️ 路线图

- [x] 用户注册/登录（JWT）
- [x] 失物/招领信息 CRUD
- [x] 认领申请与审核流程
- [x] 5种物品状态管理
- [x] 定时过期处理
- [x] 消息通知系统
- [x] 物品置顶功能
- [x] 可配置过期天数
- [x] 管理员数据仪表盘
- [x] 图片缩略图机制
- [x] 登录失败次数限制
- [x] CSS 设计变量提取
- [x] 通用组件抽取
- [ ] 管理员仪表盘图表可视化
- [ ] Redis 缓存集成
- [ ] 邮件服务接入
- [ ] Docker 容器化部署

---

## 📝 开发记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-07-24 | v1.3 | 物品置顶、可配置过期天数、系统设置、仪表盘、缩略图、登录限流 |
| 2026-07-05 | v1.2 | 关键 Bug 修复与代码规范化 |
| 2026-06-23 | v1.1 | 校园失物招领系统初始版本 |

---

## 📄 许可证

本项目仅供学习交流使用。

---

## 👨‍💻 作者

**NovaForge** — [Gitee](https://gitee.com/NovaForge/lost-found-system)
