# 模块化架构设计说明

此文档基于现代 Android 架构指南（Guide to app architecture）及项目实际需求制定。

## 1. 核心层 (core)
地基模块，无关具体业务逻辑，完全独立。
- `core_base`: 公共基础类库、常量、基础扩展。
- `core_res`: 核心层通用的资源文件（图片、颜色、字符串等）。
- `core_router`: 路由组件封装及全局跳转配置。
- `core_aar`: AAR 等本地二进制依赖集中收口和转发模块。
- `core_common`: 存放跨多模块公用的全局 Data Bean、序列化实体等。

## 2. 业务层 (feature)
按业务功能拆分的特征模块，按需独立开发和测试。
- `feature_home`: 首页、推荐、动态模块。
- `feature_login`: 登录模块。
- `feature_user`: 用户资料、设置模块。
- `feature_app`: App 业务级通用逻辑与初始化。
- `feature_res`: 业务功能层共享的资源池。

## 3. 组件库 (component)
通用或跨功能板块的基础业务能力组件，被多个 feature 依赖，避免循环依赖。
- `component_live`: 直播。
- `component_beauty`: 美颜。
- `component_pay`: 支付。
- `component_res`: 组件层独立共享资源池。

## 4. 模块间调用与路由
本项目移除了第三方路由框架依赖，使用自研轻量级 `core_router` 进行模块间的解耦调用与服务发现。
- **页面路由 (Router)**：支持 Activity、Fragment 的跨模块跳转与获取。
- **服务发现 (ServiceManager)**：支持模块间通用业务逻辑接口的稳定提供与获取。

> **详情用法请查阅：[core_router 使用说明](docs/core_router.md)**

## 5. 模块开发原则
- 每个模块独立开发、独立测试。
- 模块间通过 `Router` 或 `ServiceManager` 服务接口调用，**绝不跨模块直接依赖实现类**。
- 新增模块只需将自己的路由映射注入到全局 Router 中，调用方无需改动。
