# build-logic 使用模板

## 目标

本工程通过 `build-logic` 统一管理 Android 多模块构建规则，减少每个模块重复编写 Gradle 配置。

新模块优先使用以下插件：

```gradle
id 'wkq.android.application'
id 'wkq.android.library'
id 'wkq.android.feature'
id 'wkq.android.component'
id 'wkq.router'
```

## 新项目接入模板

新项目需要保留或复制以下内容：

```text
build-logic/
gradle/libs.versions.toml
settings.gradle
build.gradle
core/core_router_annotation
core/core_router_api
core/core_router_processor
```

`settings.gradle` 必须包含：

```gradle
pluginManagement {
    includeBuild("build-logic")

    repositories {
        maven { url = "https://jitpack.io" }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

根工程 `build.gradle` 建议包含：

```gradle
plugins {
    id 'wkq.architecture-check'
}
```

## app 模块模板

`app/build.gradle`：

```gradle
plugins {
    id 'wkq.android.application'
}

android {
    signingConfigs {
        release {
            storeFile = file('test.jks')
            storePassword = '123456'
            keyAlias = 'test'
            keyPassword = '123456'
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.release
        }
        debug {
            signingConfig = signingConfigs.release
        }
    }
}

dependencies {
    implementation project(':feature:feature_home')
    implementation project(':component:component_web')
}
```

`wkq.android.application` 已默认配置：

```text
application plugin
Kotlin Android
KSP
compileSdk/minSdk/targetSdk
applicationId/versionCode/versionName
Java 17
Kotlin jvmTarget
ViewBinding
BuildConfig
release 混淆和资源压缩
Router 生成目录
常用 AndroidX 测试依赖
常用 core 依赖
```

## feature 模块模板

适合页面、业务入口、完整业务流程。

`feature/feature_home/build.gradle`：

```gradle
plugins {
    id 'wkq.android.feature'
    id 'wkq.router'
}

android {
    namespace = 'com.wkq.home'
}

dependencies {
    implementation project(':core:core_network')
    implementation project(':component:component_web')
}
```

如果没有额外依赖，可以只写：

```gradle
plugins {
    id 'wkq.android.feature'
    id 'wkq.router'
}

android {
    namespace = 'com.wkq.home'
}
```

`wkq.android.feature` 已默认配置：

```text
Android Library
Kotlin Android
compileSdk/minSdk
Java 17
Kotlin jvmTarget
ViewBinding
consumerProguardFiles
常用 AndroidX 页面依赖
core_ui
core_base
core_util
core_user
```

`wkq.router` 已默认配置：

```text
KSP
core_router_api
core_router_processor
moduleName
```

页面路由示例：

```kotlin
@Route(path = "/home/main")
class HomeActivity : AppCompatActivity() {
}
```

路由 path 必须符合：

```text
/group/page
```

正确示例：

```text
/home/main
/user/profile
/web/main
```

错误示例：

```text
home/main
/home
/home/main/detail
```

## component 模块模板

适合可复用业务组件，例如 Web、媒体、支付、IM。

`component/component_pay/build.gradle`：

```gradle
plugins {
    id 'wkq.android.component'
    id 'wkq.router'
}

android {
    namespace = 'com.wkq.component.pay'
}

dependencies {
    implementation project(':core:core_network')
    implementation project(':core:core_user')
}
```

如果组件没有路由入口，可以不加 `wkq.router`：

```gradle
plugins {
    id 'wkq.android.component'
}

android {
    namespace = 'com.wkq.component.pay'
}
```

`wkq.android.component` 已默认配置：

```text
Android Library
Kotlin Android
compileSdk/minSdk
Java 17
Kotlin jvmTarget
ViewBinding
consumerProguardFiles
androidx.core.ktx
```

## core 模块模板

适合基础能力，例如网络、存储、UI、工具、媒体基础能力。

`core/core_logger/build.gradle`：

```gradle
plugins {
    id 'wkq.android.library'
}

android {
    namespace = 'com.wkq.logger'
}

dependencies {
}
```

如果 core 模块需要特殊插件，例如 KSP、serialization，可以单独声明：

```gradle
plugins {
    id 'wkq.android.library'
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = 'com.wkq.user'
}

dependencies {
    implementation libs.androidx.room.runtime
    implementation libs.androidx.room.ktx
    ksp libs.androidx.room.compiler
}
```

## settings.gradle include 模板

新增模块后，需要在 `settings.gradle` 中 include：

```gradle
include ':feature:feature_home'
include ':component:component_pay'
include ':core:core_logger'
```

建议目录命名：

```text
feature/feature_home
component/component_pay
core/core_logger
```

## 常用命令

检查模块依赖方向：

```powershell
.\gradlew.bat checkArchitecture --no-daemon --max-workers=1
```

打 Debug 包：

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1
```

如果遇到 Kotlin 增量缓存异常，可以先停止 Gradle：

```powershell
.\gradlew.bat --stop
```

必要时删除对应模块的 Kotlin build 缓存后重试。

## 依赖方向规则

允许：

```text
app -> feature/component/core
feature -> component/core
component -> core
core -> core
```

禁止：

```text
core -> feature
core -> component
component -> feature
feature -> feature
```

如果需要跨 feature 通信，优先使用 Router 或下沉公共接口到 `core` / `component`。

## 新建 feature 最小步骤

1. 创建目录：

```text
feature/feature_home
```

2. 添加 `feature/feature_home/build.gradle`：

```gradle
plugins {
    id 'wkq.android.feature'
    id 'wkq.router'
}

android {
    namespace = 'com.wkq.home'
}
```

3. 添加 `src/main/AndroidManifest.xml`：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

4. 在 `settings.gradle` 添加：

```gradle
include ':feature:feature_home'
```

5. 在 `app/build.gradle` 装配：

```gradle
dependencies {
    implementation project(':feature:feature_home')
}
```

6. 执行验证：

```powershell
.\gradlew.bat checkArchitecture --no-daemon --max-workers=1
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1
```
