# Router X 使用文档

Router X 是一个 Android 组件化路由库，基于 KSP 在编译期生成路由表，运行时通过 ServiceLoader 自动加载。支持页面跳转、参数传递、参数自动注入、ActivityResult、Fragment/View 获取、拦截器、服务发现和路由降级。

网络版本发布流程见：

```text
core/路由版本发布.md
```

## 1. 你需要接入哪些东西

Router X 分三部分：

```text
router-annotation   注解，例如 @Route、@Param、@ProvideService、@Interceptor
router-api          运行时 API，例如 Router、Postcard、IDegradationService
router-processor    KSP 处理器，负责生成路由注册代码
```

当前项目源码模块对应为：

```text
:core:core_router_annotation
:core:core_router_api
:core:core_router_processor
```

如果是发成 JitPack 网络库，推荐坐标为：

```text
com.github.wukuiqing49.ApplicationArchitecture:core_router_api:v1.0.0
com.github.wukuiqing49.ApplicationArchitecture:core_router_processor:v1.0.0
```

`core_router_api` 会通过 `api` 暴露 `core_router_annotation`，普通使用方只需要依赖 `core_router_api` 和 `core_router_processor`。

## 2. 本地源码引用

适合自己项目之间复用，或者路由库还没有发布到 Maven 的阶段。

### 2.1 拷贝模块

把下面三个模块复制到目标项目：

```text
core/core_router_annotation
core/core_router_api
core/core_router_processor
```

### 2.2 引入模块

在目标项目 `settings.gradle` 中添加：

```gradle
include ':core:core_router_annotation'
include ':core:core_router_api'
include ':core:core_router_processor'
```

### 2.3 配置 KSP 插件

根项目 `build.gradle`：

```gradle
plugins {
    id 'com.google.devtools.ksp' version '你的 KSP 版本' apply false
}
```

如果使用 `libs.versions.toml`：

```toml
[versions]
ksp = "你的 Kotlin 对应版本"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

注意：KSP 版本必须和 Kotlin 版本匹配。

### 2.4 在路由模块中配置

每个写了路由注解的模块都要配置：

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.devtools.ksp'
}

ksp {
    arg("moduleName", "FeatureHome")
}

dependencies {
    implementation project(":core:core_router_api")
    ksp project(":core:core_router_processor")
}
```

`moduleName` 必须在当前 App 内唯一，例如：

```text
FeatureHome
FeatureUser
ComponentWeb
ComponentPay
```

### 2.5 使用本项目的简化脚本

如果目标项目保留了本项目的 `gradle/router-module.gradle`，路由模块可以简化成：

```gradle
ext.routerModuleName = 'FeatureHome'
apply from: rootProject.file('gradle/router-module.gradle')
```

脚本内部会自动做：

```gradle
plugins.apply("com.google.devtools.ksp")

ksp {
    arg("moduleName", project.property("routerModuleName").toString())
}

dependencies {
    implementation project(":core:core_router_api")
    ksp project(":core:core_router_processor")
}
```

这个脚本只适合本地源码引用，因为它依赖 `project(":core:core_router_api")`。

## 3. 网络 Maven 库引用

适合给其他项目、其他团队或外部用户使用。

### 3.1 配置仓库

发布到 Maven Central：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

发布到私有 Maven：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://your.maven.repo/repository/releases")
        }
    }
}
```

发布到 JitPack：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 3.2 配置 KSP 插件

根项目 `build.gradle`：

```gradle
plugins {
    id 'com.google.devtools.ksp' version '你的 KSP 版本' apply false
}
```

### 3.3 在路由模块中依赖

普通 Gradle 写法：

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.devtools.ksp'
}

ksp {
    arg("moduleName", "FeatureHome")
}

dependencies {
    implementation "com.github.wukuiqing49.ApplicationArchitecture:core_router_api:v1.0.0"
    ksp "com.github.wukuiqing49.ApplicationArchitecture:core_router_processor:v1.0.0"
}
```

Version Catalog 写法：

```toml
[versions]
router = "v1.0.0"
ksp = "你的 Kotlin 对应版本"

[libraries]
router-api = { module = "com.github.wukuiqing49.ApplicationArchitecture:core_router_api", version.ref = "router" }
router-processor = { module = "com.github.wukuiqing49.ApplicationArchitecture:core_router_processor", version.ref = "router" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

模块 `build.gradle`：

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    alias(libs.plugins.ksp)
}

ksp {
    arg("moduleName", "FeatureHome")
}

dependencies {
    implementation libs.router.api
    ksp libs.router.processor
}
```

### 3.4 App 壳模块怎么依赖

如果 `app` 模块只负责初始化和跳转，不写路由注解，只需要：

```gradle
dependencies {
    implementation "com.github.wukuiqing49.ApplicationArchitecture:core_router_api:v1.0.0"
}
```

不需要写：

```gradle
ksp {
    arg("moduleName", "App")
}
```

只有 `app` 自己也写了 `@Route`、`@Param`、`@ProvideService` 或 `@Interceptor` 时，才需要配置 KSP 和 `moduleName`。

## 4. 哪些模块需要配置 moduleName

需要配置：

```text
写了 @Route 的模块
写了 @Param 的模块
写了 @ProvideService 的模块
写了 @Interceptor 的模块
```

不需要配置：

```text
只调用 Router.init() 的 app 壳模块
只调用 Router.build() 跳转的模块
没有路由注解的 core 工具模块
没有路由注解的网络、数据库、UI 基础模块
```

一句话：**谁写注解，谁配置 KSP 和 moduleName。**

## 5. 初始化

在 `Application` 中初始化一次：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Router.init(this)
    }
}
```

Debug 阶段可以打开日志和严格模式：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Router.setDebug(true)
        Router.setThrowExceptionWhenRouteNotFound(true)
        Router.init(this)
    }
}
```

Release 建议保持默认不崩，由降级服务兜底。

## 6. 定义页面路由

```kotlin
@Route(path = "/home/main")
class HomeActivity : AppCompatActivity()
```

路径建议按 `/模块/页面` 命名：

```text
/home/main
/user/detail
/web/browser
/pay/cashier
```

路径必须全局唯一。

## 7. 跳转页面

基础跳转：

```kotlin
Router.build("/home/main")
    .navigation(context)
```

携带参数：

```kotlin
Router.build("/user/detail")
    .withLong("user_id", 10001L)
    .withString("user_name", "wkq")
    .withBoolean("from_push", true)
    .navigation(context)
```

设置 Intent flags：

```kotlin
Router.build("/home/main")
    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    .navigation(context)
```

设置转场动画：

```kotlin
Router.build("/home/main")
    .withTransition(R.anim.fade_in, R.anim.fade_out)
    .navigation(context)
```

兼容旧写法：

```kotlin
Router.open("/home/main", context)
```

## 8. 获取 ActivityResult

目标 Activity 正常 `setResult()`：

```kotlin
val intent = Intent().putExtra("result_key", "ok")
setResult(Activity.RESULT_OK, intent)
finish()
```

跳转方：

```kotlin
Router.build("/user/edit")
    .navigation(activity) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val value = result.data?.getStringExtra("result_key")
        }
    }
```

这里的 `activity` 必须是 `FragmentActivity`。

## 9. 参数自动注入

目标页面：

```kotlin
@Route(path = "/user/detail")
class UserDetailActivity : AppCompatActivity() {

    @Param(name = "user_id")
    var userId: Long = 0L

    @Param
    var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Router.inject(this)
    }
}
```

跳转方：

```kotlin
Router.build("/user/detail")
    .withLong("user_id", 10001L)
    .withString("userName", "wkq")
    .navigation(context)
```

说明：

- `@Param(name = "user_id")` 使用指定 key。
- `@Param` 不写 name 时，默认使用变量名，例如 `userName`。
- `Router.inject(this)` 要在目标页面创建后调用。

## 10. 获取 Fragment 或 View

Fragment：

```kotlin
val fragment = Router.getFragment("/test/fragment", bundle)
```

View：

```kotlin
val view = Router.getView("/test/view", context)
```

对应类也需要加 `@Route`：

```kotlin
@Route(path = "/test/fragment")
class TestFragment : Fragment()

@Route(path = "/test/view")
class TestCustomView(context: Context) : FrameLayout(context)
```

## 11. 路由降级

建议每个 App 至少提供一个全局降级服务。这样路由不存在时不会直接崩溃。

```kotlin
@ProvideService(IDegradationService::class)
class AppDegradationService : IDegradationService {
    override fun onLost(context: Context, postcard: Postcard) {
        Toast.makeText(context, "页面不存在: ${postcard.path}", Toast.LENGTH_SHORT).show()
    }
}
```

这个类所在模块需要配置 KSP 和唯一的 `moduleName`。

## 12. 拦截器

拦截器适合做登录校验、权限校验、灰度控制等。

```kotlin
@Interceptor(priority = 100)
class LoginInterceptor : IInterceptor {
    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        if (isLogin()) {
            callback.onContinue(postcard)
        } else {
            callback.onInterrupt()
            Router.build("/user/login").navigation(appContext)
        }
    }
}
```

说明：

- `priority` 数值越大越先执行。
- 调用 `callback.onContinue(postcard)` 继续路由。
- 调用 `callback.onInterrupt()` 中断路由。
- 拦截器所在模块需要配置 KSP 和唯一的 `moduleName`。

## 13. 服务发现

定义服务接口：

```kotlin
interface IUserService {
    fun getUserName(): String
}
```

实现服务：

```kotlin
@ProvideService(IUserService::class)
class UserServiceImpl : IUserService {
    override fun getUserName(): String = "wkq"
}
```

获取服务：

```kotlin
val userService = Router.getService(IUserService::class)
val name = userService?.getUserName()
```

服务实现类所在模块需要配置 KSP 和唯一的 `moduleName`。

## 14. 混淆说明

Router X 支持混淆。

`router-api` 已经提供 `consumer-rules.pro`，正常源码依赖或 Maven 依赖都会自动合并到 App：

```gradle
consumerProguardFiles 'consumer-rules.pro'
```

已保护的关键内容：

```text
路由注解
生成的 RouteInit / RouteGroup 类
ServiceLoader 使用的 IRouteInit
懒加载分组 IRouteGroup
参数注入 ISyringe
包含 @Param 字段的目标类名
RouteMeta
```

当前项目开启混淆验证命令：

```bash
.\gradlew.bat :app:assembleRelease -PenableReleaseMinify=true
```

已验证混淆后 APK 中仍然存在：

```text
META-INF/services/com.wkq.router.api.IRouteInit
com.wkq.router.generated.RouteInit_FeatureTest
com.wkq.router.generated.RouteInit_ComponentWeb
```

所以路由这块混淆后不会出问题。

## 15. 推荐的最终接入方式

如果后续做成正式库，推荐再发布一个轻量 Gradle 插件：

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'com.wkq.router'
}

router {
    moduleName = "FeatureHome"
}
```

插件内部负责：

```text
apply KSP 插件
配置 ksp { arg("moduleName", "...") }
添加 router-api 依赖
添加 router-processor 的 ksp 依赖
```

这样其他项目使用时最简单。

## 16. 常见问题

### 16.1 每个模块都要配置 moduleName 吗？

不是。只有写了路由注解的模块才需要配置。

### 16.2 moduleName 可以重复吗？

不可以。`moduleName` 必须在当前 App 内唯一。

处理器会生成：

```text
RouteInit_${moduleName}
RouteGroup_${moduleName}_${groupName}
```

重复后可能导致生成类冲突。

### 16.3 打包后 Route not found 怎么查？

先检查对应模块是否配置了 KSP 和 `moduleName`。

再检查 APK 内是否存在：

```text
META-INF/services/com.wkq.router.api.IRouteInit
```

并确认里面包含对应模块生成类，例如：

```text
com.wkq.router.generated.RouteInit_FeatureHome
com.wkq.router.generated.RouteInit_ComponentWeb
```

### 16.4 本地源码引用和 Maven 引用能混用吗？

不建议混用。

本地源码引用：

```gradle
implementation project(":core:core_router_api")
ksp project(":core:core_router_processor")
```

JitPack Maven 引用：

```gradle
implementation "com.github.wukuiqing49.ApplicationArchitecture:core_router_api:v1.0.0"
ksp "com.github.wukuiqing49.ApplicationArchitecture:core_router_processor:v1.0.0"
```

同一个 App 内只保留一种方式。

### 16.5 App 只跳转页面，需要配置 KSP 吗？

不需要。只调用 `Router.init()` 或 `Router.build()` 不需要 KSP。

只有写了 `@Route`、`@Param`、`@ProvideService`、`@Interceptor` 才需要 KSP。
