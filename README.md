# 超市收银系统

一个基于 Spring Boot + Vue 3 的现代化超市收银管理系统，提供完整的商品管理、收银结算、会员管理、进货管理、统计分析等功能。

## 说明

前端代码暂未开源，代码不易，如有需要添加QQ：384287067

## 功能特性

- **收银台**：支持扫码录入、数量修改、会员结算、多种支付方式
- **商品管理**：商品信息维护、分类管理、库存预警
- **会员管理**：会员信息管理、余额充值、积分折扣
- **订单管理**：订单查询、订单详情、销售记录
- **进货管理**：进货记录、供应商管理
- **统计分析**：销售趋势、商品排行、收银员业绩
- **系统管理**：用户管理、权限控制

## 页面展示

### 登录页

![登录页](doc/image/登录页.png)

### 首页看板

![首页看板](doc/image/首页看板.png)

### 收银台

![收银台](doc/image/收银台.png)

### 接口文档

![接口文档](doc/image/接口文档展示.png)

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.4 | 基础框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.37.0 | 权限认证 |
| MySQL | - | 数据库 |
| Druid | 1.2.21 | 数据库连接池 |
| Knife4j | 4.4.0 | 接口文档 |
| Hutool | 5.8.25 | 工具库 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.21 | 前端框架 |
| Vite | 5.2.0 | 构建工具 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.0 | 路由管理 |
| Naive UI | 2.38.1 | UI 组件库 |
| Axios | 1.6.8 | HTTP 请求 |
| ECharts | 5.5.0 | 图表库 |
| Day.js | 1.11.10 | 日期处理 |

## 项目结构

```
Supermarket-Cashier-System/
├── cashier-backend/                # 后端项目
│   ├── src/main/java/com/cashier/
│   │   ├── common/                 # 公共模块
│   │   │   ├── config/             # 配置类
│   │   │   ├── constant/           # 常量
│   │   │   ├── dto/                # 公共DTO
│   │   │   ├── exception/          # 异常处理
│   │   │   ├── result/             # 统一响应
│   │   │   └── utils/              # 工具类
│   │   └── module/                 # 业务模块
│   │       ├── auth/               # 认证模块
│   │       ├── goods/              # 商品模块
│   │       ├── member/             # 会员模块
│   │       ├── order/              # 订单模块
│   │       ├── purchase/           # 进货模块
│   │       ├── statistics/         # 统计模块
│   │       ├── supplier/           # 供应商模块
│   │       └── user/               # 用户模块
│   └── src/main/resources/
│       ├── db/                     # 数据库脚本
│       ├── mapper/                 # MyBatis映射文件
│       └── application.yml         # 配置文件
│
└── cashier-frontend/               # 前端项目
    ├── src/
    │   ├── api/                    # API 接口
    │   ├── assets/                 # 静态资源
    │   ├── components/             # 公共组件
    │   ├── layout/                 # 布局组件
    │   ├── router/                 # 路由配置
    │   ├── stores/                 # 状态管理
    │   ├── utils/                  # 工具函数
    │   └── views/                  # 页面组件
    └── vite.config.js              # Vite 配置
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### 后端启动

1. 创建数据库并导入初始数据

```bash
mysql -u root -p < cashier-backend/src/main/resources/db/init.sql
```

2. 修改数据库配置

编辑 `cashier-backend/src/main/resources/application-dev.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cashier_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

3. 启动后端服务

```bash
cd cashier-backend
mvn spring-boot:run
```

后端服务启动后访问：
- 接口地址：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html

### 前端启动

1. 安装依赖

```bash
cd cashier-frontend
npm install
```

2. 启动开发服务器

```bash
npm run dev
```

3. 构建生产版本

```bash
npm run build
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 收银员 | cashier01 | 123456 |
| 收银员 | cashier02 | 123456 |

## 数据库设计

系统包含以下主要数据表：

| 表名 | 说明 |
|------|------|
| sys_user | 系统用户表 |
| goods_category | 商品分类表 |
| goods | 商品表 |
| member | 会员表 |
| sale_order | 销售订单表 |
| sale_order_item | 订单明细表 |
| supplier | 供应商表 |
| purchase_record | 进货记录表 |

## API 文档

启动后端服务后，访问 Knife4j 接口文档：http://localhost:8080/doc.html

## 许可证

MIT License
