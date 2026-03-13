# 🏗️ 项目架构说明 (Project Architecture)

本项目采用 **分层模块化架构（Layered Modular
Architecture）**，通过清晰的模块划分来降低耦合、提升代码复用率，并支持业务快速扩展。

该架构适用于 **中大型 Android
项目**，能够在项目规模不断增长的情况下保持代码结构清晰、可维护性强。

------------------------------------------------------------------------

# 📦 模块结构

    ApplicationArchitecture
    ├── app                         # 应用入口模块
    │
    ├── core                        # 核心基础层
    │   ├── core_aar               # AAR库支持
    │   ├── core_base              # 基础框架
    │   ├── core_network           # 网络层
    │   ├── core_ui                # UI基础组件
    │   ├── core_util              # 工具库
    │   ├── core_res               # 公共资源
    │   └── core_router            # 路由框架
    │
    ├── component                   # 可复用业务组件层
    │   ├── component_live         # 直播组件
    │   ├── component_beauty       # 美颜组件
    │   ├── component_pay          # 支付组件
    │   └── component_res          # 组件资源
    │
    └── feature                     # 业务功能模块
        ├── feature_app            # 主业务功能
        ├── feature_res            # 资源业务
        └── feature_test           # 测试功能

------------------------------------------------------------------------

# 📊 架构分层说明

层级              作用
  ----------------- ----------------------------------------
App Layer         应用入口与模块集成
Core Layer        提供基础能力（网络、UI、工具、路由等）
Component Layer   可复用业务组件
Feature Layer     具体业务功能实现

依赖关系原则：

    Feature  → Core
    Component → Core
    App → Feature + Component + Core

该规则保证：

-   业务模块之间不会直接耦合
-   所有基础能力统一由 Core 提供
-   App 只负责集成和启动

------------------------------------------------------------------------

# ⚙️ 技术栈

技术          版本
  ------------- ---------------------
语言          Kotlin
Compile SDK   36
Min SDK       24
Target SDK    36
JVM           Java 17
构建工具      Gradle (Kotlin DSL)

------------------------------------------------------------------------

# 🚀 自动路由机制

项目实现了 **自动路由注册系统**，用于实现模块间解耦通信。

工作流程：

    Feature / Component
            │
            ▼
          *Routes.kt
            │
            ▼
       Gradle 扫描任务
            │
            ▼
    生成 RouterInit.kt
            │
            ▼
    应用启动时统一注册

主要特性：

-   自动扫描所有 `*Routes.kt`
-   自动生成 `RouterInit.kt`
-   自动注册 Feature / Component 模块路由
-   模块间通过 Router 解耦通信

------------------------------------------------------------------------

# 🔧 构建自动化

项目在 Gradle 中实现了多种自动化能力。

## 自动包含模块

系统会自动加载以下模块：

    :feature:*
    :component:*

新增模块无需手动在 `app` 中添加依赖。

------------------------------------------------------------------------

## APK 自动命名

构建 APK 时会自动生成带版本和时间的文件名：

    ApplicationArchitecture_${buildType}_v${versionName}_${time}.apk

示例：

    ApplicationArchitecture_release_v1.0.0_20260313.apk

------------------------------------------------------------------------

# 🎯 架构优势

### 低耦合

各模块之间通过 Router 通信，避免模块直接依赖。

### 高复用

Component 层组件可以被多个 Feature 复用。

### 易扩展

新增业务只需添加新的 Feature 模块。

### 自动化构建

减少手动配置与依赖维护。

### 职责清晰

每一层只负责自己的职责。

------------------------------------------------------------------------

# 🧠 架构设计理念

本项目结合了多种现代 Android 架构思想：

-   **Modular Architecture（模块化架构）**
-   **Layered Architecture（分层架构）**
-   **Feature-based Development（按功能拆分模块）**

这种设计可以让项目在代码规模增长到 **10万行甚至更高**
时，依然保持清晰的结构和良好的可维护性。
