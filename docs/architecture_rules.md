# 模块化架构规则

## 分层目标

本工程按 `app -> feature/component -> core` 组织代码，目标是让业务模块可以独立开发、独立替换，并通过 Router 或服务接口完成跨模块通信。

## 模块职责

- `app`：壳工程，只负责应用装配、全局初始化、打包配置。
- `feature`：完整业务功能或页面集合，例如首页、测试入口、订单、个人中心。
- `component`：可复用业务组件，例如 Web 容器、媒体选择、支付、IM。
- `core`：基础能力，例如 base、network、storage、router、ui、util、user。

## 依赖方向

允许：

- `app` 依赖 `feature`、`component`、`core`
- `feature` 依赖 `component`、`core`
- `component` 依赖 `core`
- `core` 内部按职责少量依赖其他 `core` 模块

禁止：

- `core` 依赖 `feature` 或 `component`
- `feature` 直接依赖其他 `feature`
- `component` 依赖 `feature`
- 跨业务模块直接调用具体实现类

## 跨模块通信

- 页面跳转使用 `Router.open()` 或 `Router.build().navigation()`。
- 跨模块业务能力使用 `@ProvideService` 暴露接口。
- 路由路径必须符合 `/group/page` 格式，例如 `/test/main`、`/common/webview`。
- 新增带路由的模块优先应用 `wkq.router` 插件，不要手写 KSP 依赖和 `moduleName`。

## 自动检查

执行以下命令检查依赖方向：

```bash
./gradlew checkArchitecture
```

如果发现非法依赖，需要优先通过接口下沉、Router 服务发现或拆分公共能力解决，不要绕过检查。
