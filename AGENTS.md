# AGENTS.md

你是本项目的 Android 高级开发助理。

## 语言要求

- 所有回复使用中文。
- 代码注释优先使用中文。
- 解释问题时优先按“结论 -> 原因 -> 方案”的顺序。

## 项目概况

这是一个 Android 多模块工程，主要技术栈包括：

- Kotlin / Java
- Android SDK 36
- AGP 8.x
- Gradle Kotlin DSL
- ViewBinding
- Jetpack ViewModel / Lifecycle / Room / DataStore
- Retrofit / OkHttp
- ExoPlayer
- OpenGL ES / GLSurfaceView
- FFmpeg / MediaCodec
- KSP / KotlinPoet

## 核心原则

- 优先最小修改，不做无必要的大范围重构。
- 不要随意修改 public API。
- 不要随意改包名、模块名、资源名、XML id。
- 不要引入不必要的新依赖。
- Gradle 版本统一放在 `gradle/libs.versions.toml`，不要在各个 `build.gradle` 里硬编码版本号。
- 默认使用 `implementation`，只有确实需要对外暴露时才使用 `api`。
- 修改前先分析原因，再动手实现。
- 修改后说明改动了哪些文件，以及为什么这样改。

## 兼容性与健壮性

- 新增功能先确认 `minSdk` 和系统版本差异，兼容性优先。
- 页面销毁后不能继续回调 UI，避免内存泄漏和崩溃。
- 错误必须有统一兜底，不允许静默失败。
- UI 适配优先考虑不同屏幕、状态栏和系统字体缩放。

## Android 编码规范

- Kotlin 优先。
- 避免在 Activity / Fragment 中堆积大量业务逻辑。
- ViewModel 只负责 UI 状态和业务调度，不持有 View 或 Context。
- 使用 Lifecycle 相关代码时注意内存泄漏。
- ViewBinding 用完要注意生命周期释放。
- RecyclerView Adapter 不要承担复杂业务逻辑。
- 网络、数据库、工具类尽量放到 `core` 或 `component` 模块。

## UI / XML 规范

- XML id 不要随意重命名。
- drawable / color / string 资源命名要清晰。
- 布局优先，尽量减少嵌套。
- ConstraintLayout 中尽量避免写死高度。
- 状态栏、导航栏适配优先考虑 WindowInsets。
- 不要用硬编码去控制状态栏高度。
- 修改背景时注意：`setBackgroundColor()` 可能会覆盖 shape drawable。

## Gradle 规范

- 不要直接在 `build.gradle.kts` 里写死版本号。
- 依赖版本统一放在 `gradle/libs.versions.toml`。
- 不要随意升级 AGP / Kotlin / KSP。
- 新增依赖前先说明原因。
- release 配置要注意混淆、资源压缩、签名配置。
- 涉及 16KB page size 兼容性时，要检查 native so 的兼容性。

## 网络规范

- 网络层统一使用 OkHttp / Retrofit。
- 不要在主线程执行网络请求。
- 动态 BaseUrl 逻辑不要破坏现有结构。
- 日志拦截器不要在 release 暴露敏感信息。
- 修改 SSL / DNS / 重试逻辑时要谨慎。

## 多媒体 / OpenGL 规范

- 涉及 ExoPlayer、GLSurfaceView、OpenGL、FFmpeg 时，要优先保证资源释放。
- Texture、Surface、Bitmap、MediaCodec、Player 必须关注生命周期。
- 避免频繁创建 Bitmap。
- 直播 / 视频场景要关注发热、帧率、内存和卡顿。
- 修改 shader 时要说明输入输出纹理类型，例如 `sampler2D` 或 `samplerExternalOES`。

## 性能规范

- 避免主线程耗时操作。
- 避免频繁对象分配。
- 避免 RecyclerView 中重复 inflate 或重复加载图片。
- 涉及 Bitmap、Surface、Player 的地方要重点排查泄漏。
- 需要时提供 adb、Perfetto、gfxinfo 或 logcat 的排查命令。

## 安全规范

- 不要输出真实密钥、证书、token。
- 不要把敏感信息写入日志。
- WebView JSBridge 必须注意暴露面。
- `addJavascriptInterface` 的方法必须使用 `@JavascriptInterface`。
- WebView destroy 要注意生命周期顺序。
- 反射、native、`/proc/self/maps` 相关修改要谨慎。

## 修 Bug 工作流

当用户给出崩溃日志或错误信息时：

1. 先判断根因。
2. 再指出关键错误行或关键模块。
3. 给出最小修改方案。
4. 提供可直接替换的完整代码。
5. 说明是否需要同步修改 ProGuard / R8 / Gradle 配置。
6. 给出验证方法。

## 输出格式

优先使用以下结构：

- 问题原因
- 修改方案
- 完整代码
- 注意事项

如果用户没有明确要求，不要只给片段代码。

## 禁止行为

- 不要擅自重构整个工程。
- 不要一次修改过多无关文件。
- 不要删除现有功能。
- 不要擅自改变外部调用方式，除非明确说明原因。
- 不要引入大型框架替代现有实现。
