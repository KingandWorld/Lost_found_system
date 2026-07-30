# 🎒 校园失物招领系统

基于 Spring Boot + Vue3 的校园失物招领管理平台，支持失物发布、招领发布、认领申请、审核流程、积分会员等功能。

> **最新版本**：V1.2 | **发布日期**：2026-07-31

---

## 📋 功能概览

### 前台（学生端）
- 🔍 失物/招领信息浏览与搜索
- 📝 发布失物信息、发布招领信息
- 🤝 认领申请 / 归还申请
- 🔔 消息通知（申请、审核、状态变更）
- 👤 个人中心（信息修改、头像上传、密码修改）
- ⭐ 积分兑换会员

### 后台（管理员端）
- 📊 数据统计面板
- 👥 用户管理（角色管理、状态管理）
- 📦 失物/招领信息管理（编辑、删除、状态变更）
- ✅ 认领申请审核
- ⚙️ 系统设置（验证码开关、积分规则、过期天数）

---

## 🛠 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.2 + Java 17 |
| **ORM** | MyBatis-Plus |
| **数据库** | MySQL 8 |
| **安全** | Spring Security + JWT |
| **API文档** | Swagger / Knife4j |
| **前端框架** | Vue 3 (Composition API) |
| **UI库** | Element Plus |
| **状态管理** | Pinia |
| **构建工具** | Vite 4 |
| **路由** | Vue Router 4 |

---

## 📁 项目结构

```
失物招领系统/
├── springboot/                    # 后端 Spring Boot
│   ├── src/main/java/org/example/springboot/
│   │   ├── config/                # 安全、JWT、跨域等配置
│   │   ├── controller/            # REST 接口层
│   │   ├── service/               # 业务逻辑层
│   │   ├── mapper/                # MyBatis 数据访问层
│   │   ├── entity/                # 数据实体
│   │   ├── DTO/                   # 数据传输对象
│   │   ├── enumClass/             # 枚举类
│   │   ├── exception/             # 全局异常处理
│   │   └── util/                  # 工具类
│   └── src/main/resources/
│       ├── application.properties # 主配置
│       └── db/migration/          # 数据库迁移脚本
├── vue3/                          # 前端 Vue3
│   └── src/
│       ├── views/
│       │   ├── frontend/          # 前台页面
│       │   ├── backend/           # 后台页面
│       │   └── auth/              # 登录/注册
│       ├── components/            # 公共组件
│       ├── store/                 # Pinia 状态管理
│       ├── router/                # 路由配置
│       └── utils/                 # 工具函数
└── 项目打包/                       # 部署产物
    ├── backend/                   # JAR包 + 启动脚本
    ├── frontend/dist/             # 前端静态文件
    ├── database/                  # 数据库SQL
    └── uploads/                   # 上传文件目录
```

---

## 🚀 本地开发

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 初始化数据库

```bash
mysql -u root -p < 项目打包/database/lost_found_db.sql
```

### 2. 启动后端

```bash
cd springboot
# 修改 application.properties 中的数据库密码
./mvnw spring-boot:run -DskipTests
# 后端运行在 http://localhost:1235
# Swagger 文档：http://localhost:1235/api/doc.html
```

### 3. 启动前端

```bash
cd vue3
npm install
npm run dev
# 前端运行在 http://localhost:3002
```

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |
| 普通用户 | 自行注册 | — |

---

## 📦 生产部署

1. **后端**：将 `项目打包/backend/springboot-0.0.1-SNAPSHOT.jar` + `application-prod.properties` + `startup.sh` 上传到服务器
2. **前端**：将 `项目打包/frontend/dist/` 部署到 Nginx
3. **环境变量**：设置 `DB_PASSWORD`、`SPRING_SECURITY_PASSWORD`、`USER_DEFAULT_PASSWORD`

详见 `项目打包/云服务器部署.md`

---

## 📝 版本历史

| 版本 | 日期 | 说明 |
|:----:|:----:|------|
| **V1.2** | 2026-07-31 | Bug修复补充：昵称修改、头像5MB、图片预览统一、搜索回退持久化、失物"申请归还"文案、状态变更通知级联 |
| **V1.1** | 2026-07-30 | 修复V1.0全部10个Bug（1致命/2严重/2一般/5建议） |
| **V1.0** | 2026-07-29 | 初始发布版 |

### V1.1 修复清单

| Bug ID | 严重程度 | 问题 | 状态 |
|:------:|:--------:|------|:----:|
| BUG-008 | 🔴致命 | 删除物品"内部服务器错误" | ✅ |
| BUG-002 | 🟠严重 | 邮箱无法登录 | ✅ |
| BUG-003 | 🟠严重 | 用户不存在提示暴露 | ✅ |
| BUG-001 | 🟡一般 | 用户协议未勾选可注册 | ✅ |
| BUG-004 | 🟡一般 | 无法修改昵称 | ✅ |
| BUG-005 | 🟢建议 | 头像大小限制2MB不一致 | ✅ |
| BUG-006 | 🟢建议 | 新旧密码相同无提示 | ✅ |
| BUG-007 | 🟢建议 | 丢失时间可选未来日期 | ✅ |
| BUG-009 | 🟢建议 | 失物图片无法放大预览 | ✅ |
| BUG-010 | 🟢建议 | 搜索后浏览器回退状态清空 | ✅ |

---

## 🔗 仓库地址

- **Gitee**：[https://gitee.com/novaforge/lost-found-system](https://gitee.com/novaforge/lost-found-system)
- **GitHub**：[https://github.com/KingandWorld/Lost_find_system](https://github.com/KingandWorld/Lost_find_system)

---

> 🤖 文档生成：Claude Code | 最后更新：2026-07-31
