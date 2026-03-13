🏗️ 架构分层结构
Code




ApplicationArchitecture
├── 🎯 App 层 (应用层)
│   └── :app (主应用程序入口)
│
├── 🔧 Core 层 (核心基础层) - 7个核心模块
│   ├── :core:core_aar          (AAR库支持)
│   ├── :core:core_base         (基础库)
│   ├── :core:core_network      (网络层)
│   ├── :core:core_ui           (UI基础组件)
│   ├── :core:core_util         (工具库)
│   ├── :core:core_res          (资源库)
│   └── :core:core_router       (路由框架)
│
├── 🧩 Component 层 (业务组件库) - 4个可复用组件
│   ├── :component:component_live    (直播组件)
│   ├── :component:component_beauty  (美颜组件)
│   ├── :component:component_pay     (支付组件)
│   └── :component:component_res     (资源组件)
│
└── ⚙️ Feature 层 (具体业务层) - 3个业务功能
├── :feature:feature_app    (主应用业务)
├── :feature:feature_res    (资源业务)
└── :feature:feature_test   (测试功能)
📊 架构特点
1. 分层设计
   App层: 主应用程序入口和集成点
   Core层: 提供基础设施支持（网络、UI、路由、工具等）
   Component层: 可复用的业务组件（直播、美颜、支付等）
   Feature层: 具体的业务功能实现
2. 关键技术栈
   Code
   ✅ 语言: Kotlin 100%
   ✅ 编译SDK: 36 (Android 16)
   ✅ 最小SDK: 24 (Android 7.0)
   ✅ 目标SDK: 36
   ✅ JVM版本: Java 17
   ✅ 构建工具: Gradle Kotlin DSL
3. 智能路由机制
   您的项目实现了 自动路由注册系统：

✨ 自动扫描所有 *Routes.kt 文件
✨ 动态生成 RouterInit.kt
✨ 支持Feature和Component模块的自动注册
✨ 实现了解耦的模块化通信
4. 自动化构建特性
   Gradle
   // app/build.gradle 中的自动化实现:
- 自动包含所有 :feature: 和 :component: 模块
- 动态版本命名 (QdVoice_${buildType}_v${versionName}_${time}.apk)
- 自动路由代码生成
- Native库管理和扫描
5. 项目配置
   应用包名: com.qianrun.voice
   版本号: 1001 (自动转换为 1.0.0.1)
   签名配置: 已配置Release签名证书
   混淆配置: Release版本启用ProGuard混淆和资源压缩
   🎯 架构优势
   优势	说明
   高内聚，低耦合	各层模块独立开发，通过Router通信
   易于扩展	添加新Feature或Component无需修改主应用
   代码复用	Component层组件可被多个Feature复用
   自动化构建	减少手动配置和集成成本
   清晰的职责划分	Core负责基础，Component提供业务块，Feature实现具体功能
   📝 核心依赖关系
   Code
   :app (主应用)
   ├── 依赖 :core:core_base
   ├── 依赖 :core:core_ui
   ├── 依赖 :core:core_network
   ├── 依赖 :core:core_router
   └── 自动依赖所有 :feature:* 和 :component:*