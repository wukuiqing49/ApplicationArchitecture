# Router X 路由库使用指南

Router X 是一个基于 KSP (Kotlin Symbol Processing) 和 SPI (Service Provider Interface) 机制构建的高性能、组件化路由框架。支持 Activity 跳转、Fragment/View 动态获取、参数携带、拦截器及服务发现功能。

## 1. 核心架构

项目采用三层架构，确保编译性能与代码解耦：
- **`router_annotation`**: 定义路由协议、注解（`@Route`, `@Interceptor`, `@ProvideService`）。
- **`router_processor`**: KSP 插件，扫描注解并自动生成注册代码，通过 SPI 机制解耦。
- **`router_api`**: 运行时门面，提供拦截、跳转、组件获取等核心实现。

## 2. 模块配置 (Module Configuration)

要在新的模块中启用路由功能，需在该模块的 `build.gradle` (或 `build.gradle.kts`) 中添加以下配置：

### 2.1 引入 KSP 插件
```gradle
plugins {
    alias(libs.plugins.ksp) // 确保在 version catalog 中定义了 ksp 插件
}
```

### 2.2 配置模块名 (必填)
每个模块必须配置唯一的 `moduleName` 参数，供 KSP 生成对应的初始化类：
```gradle
ksp {
    arg("moduleName", "YourModuleName") // 如 "User", "Order", "FeatureLive"
}
```

### 2.3 添加依赖
```gradle
dependencies {
    // 基础库依赖（含有注解与跳转接口）
    implementation project(':router:router_api')
    
    // 编译器依赖（负责生成代码）
    ksp project(":router:router_processor")
}
```

## 3. 核心架构

### 3.1 初始化
在 `Application` 的 `onCreate` 中调用：
```kotlin
Router.init(this)
```
*注：框架使用 SPI 机制，会自动加载所有 Module 中生成的注册表，无需手动配置模块名。*

### 3.2 定义路由 (Activity/Fragment/View)

使用 `@Route` 注解标记组件，并指定唯一路径（Path）：
```kotlin
@Route(path = "/feature/main")
class MainActivity : BaseActivity<ActivityMainBinding>() { ... }

@Route(path = "/feature/fragment")
class MyFragment : Fragment() { ... }

@Route(path = "/feature/view")
class CustomView(context: Context) : FrameLayout(context) { ... }
```

### 3.3 跳转 Activity

#### 基础跳转
```kotlin
Router.open("/feature/main", context)
```

#### 携带参数与动画
```kotlin
Router.build("/feature/main")
    .withString("key", "value")
    .withInt("id", 123)
    .withTransition(R.anim.fade_in, R.anim.fade_out) // 内部适配 Android 14+ 新 API
    .navigation(context)
```

#### 携带 Result 回调 (推荐)
无需配置 `ActivityResultLauncher`，直接通过闭包获取结果：
```kotlin
Router.build("/feature/pick")
    .navigation(activity) { result ->
        val data = result.data?.getStringExtra("picked_info")
        // 处理结果...
    }
```

### 3.4 参数自动注入 (@Param)

Router X 支持 KSP 编译时生成的自动注入逻辑，免去手动编写 `intent.getXXX()` 的繁琐。

#### 第一步：在目标 Activity/Fragment 中定义变量
使用 `@Param` 注解标记变量（支持 `val` 或 `var`，推荐 `var` 以便注入）：
```kotlin
@Route(path = "/user/detail")
class UserDetailActivity : BaseActivity<ActivityUserDetailBinding>() {

    @Param(name = "user_id") // 指定 Key，若不指定则默认使用变量名
    var userId: Long = 0L

    @Param
    var userName: String? = null

    @Param
    var tags: IntArray? = null // 支持基本类型数组、Parcelable 及 Serializable
}
```

#### 第二步：在 `onCreate` 中触发注入
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Router.inject(this) // 一行代码完成注入
    // 此后 userId, userName 等变量已自动赋值
}
```
*注：框架内部对 Syringe 类有缓存机制，高性能且线程安全。*

## 4. 拦截器 (Interceptor)

用于全局导航控制（如登录状态校验）。

### 4.1 定义拦截器
支持 **优先级 (Priority)** 机制，数值越大越先执行：
```kotlin
@Interceptor(priority = 100)
class AuthInterceptor : IInterceptor {
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        if (isLogin) {
            callback.onContinue(postcard)
        } else {
            // 中断并重定向
            callback.onInterrupt(null)
            Router.open("/user/login", postcard.context)
        }
    }
}
```

## 5. 全局降级与容错 (Degradation)

当路由路径不存在（如拼写错误或版本兼容）时，App 不会崩溃，而是触发降级服务。

### 5.1 实现降级服务
```kotlin
@ProvideService(IDegradationService::class)
class MyDegradation : IDegradationService {
    override fun onLost(context: Context, postcard: Postcard) {
        // 跳转 H5 错误页或 Toast 提示
        Toast.makeText(context, "页面迷路了: ${postcard.path}", Toast.LENGTH_SHORT).show()
    }
}
```

## 6. 组件与服务发现

### 6.1 获取 UI 实例
支持跨模块获取 UI 组件实例，实现 UI 解耦。
```kotlin
val fragment = Router.getFragment("/feature/fragment", bundle)
val customView = Router.getView("/feature/view", context)
```

### 6.2 服务/工具类获取
通过路由解耦跨模块的业务逻辑调用。

#### 定义服务接口
```kotlin
interface IUserService {
    fun getUserName(): String
}
```

#### 实现并注册服务
使用 `@ProvideService` 标记实现类：
```kotlin
@ProvideService(IUserService::class)
class UserServiceImpl : IUserService {
    override fun getUserName() = "Antigravity"
}
```

#### 获取并使用服务
```kotlin
val userService = Router.getService(IUserService::class)
val name = userService?.getUserName()
```

## 7. 注意事项与排错

1.  **路径重复**：Path 必须全局唯一，否则会报错或覆盖。
2.  **Route not found**：
    - 检查是否添加了 `@Route`。
    - 检查路径拼写是否正确。
    - 确保 `KSP` 的 `RouteProcessor` 已成功生成代码。
3.  **KSP 增量编译**：如果修改了注解但未生效，请尝试执行 `./gradlew clean`。

---
*Router X - 构建更健壮、高性能的 Android 组件化架构。*
