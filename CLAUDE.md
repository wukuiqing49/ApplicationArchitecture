# CLAUDE.md

本文件与 `AGENTS.md` 保持一致，供支持 `CLAUDE.md` 的工具读取。

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

## UI 基础能力约定

- Tab + 列表/分页类页面优先使用 `MagicIndicator + ViewPager2 + Fragment` 组合实现，不要在业务页面重复手写一套联动逻辑。
- `MagicIndicator + ViewPager2` 的联动逻辑优先沉到 `core_ui`；业务页面只负责提供标题、Fragment 和样式配置。
- `ViewPager2 + Fragment` 优先使用 `BaseFragmentStateAdapter` 或后续统一封装的 Tab Adapter，不要在业务页面重复写匿名 `FragmentStateAdapter`。
- 圆形、圆角图片统一使用 Material `ShapeableImageView`，优先复用 `core_ui` 中的 `ShapeableImageView_R5/R10/R15/R20/Circle` 等样式，不要用 Bitmap 裁剪或自定义 ImageView 重复实现。
- 隐藏滑动条/滚动条这类视觉行为必须走统一工具或统一控件封装，避免在页面里到处散写 `isVerticalScrollBarEnabled = false`、`isHorizontalScrollBarEnabled = false`、`overScrollMode = never` 等临时代码。
- RecyclerView 默认应隐藏滚动条并按需关闭边缘回弹效果，但不能禁止内容滑动，也不能影响 Adapter 数据刷新和 Item 点击。
- ScrollView / NestedScrollView 默认应隐藏滚动条并按需关闭边缘回弹效果，不能破坏子 View 点击、输入框聚焦和无障碍事件。
- WebView 默认应隐藏滚动条并按需关闭边缘回弹效果，相关封装应放在 `component_web` 或公共工具中，不能破坏 WebView 生命周期、文件选择、JSBridge 和页面销毁流程。
- ViewPager2 / 老 ViewPager 默认应隐藏滚动条/滑动指示器；只有业务明确要求禁止用户切页时，ViewPager2 才使用 `isUserInputEnabled = false`，不要把“隐藏滑动条”误实现成“禁止滑动”。
- 隐藏滚动条是默认视觉规范；长文阅读、PDF、Web 内容页等确实需要滚动位置提示的场景，可以按业务保留滚动条。
- 文档中提到的统一封装如果当前项目尚未实现，不要在业务页面假装调用不存在的 API；应先把通用能力沉到合适模块并增加 demo，再在业务页面使用。
- `core_base` 负责 Activity、Fragment、Adapter、Insets、权限、通用弹框、空布局等基础结构；`core_ui` 负责 MagicIndicator、ShapeableImageView 样式、滑动控制工具、自定义 View 和纯 UI helper。
- `component_web` 负责 WebView 生命周期、文件选择、JSBridge、安全策略和 WebView 默认 UI 行为；`feature_test` 只放测试入口和 demo，不承载可复用基础封装。

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
