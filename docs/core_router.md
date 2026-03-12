# core_router 使用说明

本项目移除了第三方路由框架依赖，使用自研轻量级 `core_router` 进行模块间的解耦调用与服务发现。

## 1. 页面路由 (Router)
`Router` 用于 Activity、Fragment 等页面的跨模块跳转及获取。

**注册路由 (在目标模块的初始化块或 Application 中注册)**
```kotlin
// 注册 Activity
Router.add(Route.activity("/login/main", LoginActivity::class))

// 注册 Fragment
Router.add(Route.fragment("/home/fragment") { HomeFragment() })
```

**页面跳转 / 获取页面**
```kotlin
// 跨模块打开 Activity，并支持 DSL 传递 Intent 参数
Router.open("/login/main", context) {
    putExtra("key", "value")
}

// 跨模块获取 Fragment 实例
val fragment = Router.getFragment("/home/fragment")
```

## 2. 服务发现 (ServiceManager)
`ServiceManager` 用于模块间通用业务逻辑接口的提供与获取。

**定义与注册服务**
```kotlin
// 1. 在 core 或 public module 定义接口
interface IUserService {
    fun getUserName(): String
}

// 2. 在业务模块中实现并注册
class UserServiceImpl : IUserService {
    override fun getUserName() = "Qianrun"
}
ServiceManager.register(IUserService::class.java, UserServiceImpl())
```

**获取并使用服务**
```kotlin
// 3. 在任何模块中跨模块调用
val userService = ServiceManager.get(IUserService::class.java)
val name = userService?.getUserName()
```
