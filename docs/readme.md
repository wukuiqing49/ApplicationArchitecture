# 🏗️ 项目架构说明 (Project Architecture)

本项目采用 **分层模块化架构（Layered Modular Architecture）**，通过清晰的模块划分来降低耦合、提升代码复用率，并支持业务快速扩展。

该架构适用于 **中大型 Android 项目**，能够在项目规模不断增长的情况下保持代码结构清晰、可维护性强。

------------------------------------------------------------------------

# 📦 模块结构

```text
ApplicationArchitecture
├── app                         # 应用入口模块
│
├── core                        # 核心基础层
│   ├── core_aar                # AAR库支持
│   ├── core_base               # 基础框架
│   ├── core_data               # 数据管理层 (UserInfo等)
│   └── core_network            # 网络层
│
├── router                      # Router X 路由组件 (New)
│   ├── router_annotation       # 路由注解协议
│   ├── router_api              # 运行时核心库
│   └── router_processor        # KSP 编译期插件
│
├── component                   # 可复用业务组件层
│   ├── component_live          # 直播组件
│   ├── component_beauty        # 美颜组件
│   ├── component_pay           # 支付组件
│   └── component_res           # 组件资源
│
└── feature                     # 业务功能模块
    ├── feature_app             # 主业务功能
    ├── feature_res             # 资源业务
    └── feature_test            # 测试功能
```

------------------------------------------------------------------------

# 📊 架构分层说明

| **Core Layer** | 提供基础能力（数据、网络、UI、工具、路由等） |

---

## 🛠️ 核心模块亮点 (Core Module Highlights)

### 🛰️ core_network (网络请求模块)
- **统一响应处理**: 通过 `GlobalNetHandler` 实现全局业务 Code 拦截（如 401 Token 过期自动处理）。
- **协程化接口**: 提供 `awaitResult()` 扩展，支持 `.onSuccess { ... }.onError { ... }` 链式调用，告别嵌套回调。
- **文件下载**: 集成 `Flow` 实时回调下载进度，支持大数据流式下载。
- **详尽备注**: 全模块支持详尽的中文备注，方便快速上手。

### 👤 core_user (用户/账号管理)
- **多账号体系**: 支持本地多账号存储与快速切换，确保并行请求下的互斥读取（使用 `Mutex` 锁预防缓存击穿）。
- **实时同步**: 利用 `StateFlow` (+ `stateIn`) 实现内存缓存与数据库状态的实时联动，全局单例响应式监听。
- **存储方案**: 采用 `Room` (支持 Schema 导出验证) + `MMKV` 缓存基础信息，并使用 `ConcurrentHashMap` 确保内存操作的线程安全。
- **高内聚封装**: 严格控制类可见性，仅公开 `UserManager` 和 `UserEntity`，其余实现逻辑标记为 `internal`。

---

### 🚀 Router X (组件化路由框架)
- **router_annotation**: 纯 Kotlin 模块，定义 `@Route`, `@Param`, `@ProvideService`, `@Interceptor` 等通用接口，支持多端复用。
- **router_processor**: 基于 KSP 的代码生成引擎，在编译期扫描注解并输出路由表与注入类，确保零反射、高性能。
- **router_api**: 运行时外观类，提供拦截器链路、属性自动注入、分组懒加载及跨模块服务发现等高级特性。

---

**依赖关系原则**：
- `Feature` → `Core` & `router_api`
- `Component` → `Core` & `router_api`
- `App` → `Feature` + `Component` + `Core` + `router_api`
- `All` → `router_processor` (via KSP)

---

# 📜 开发规范 (Conventions)

### 1. 资源命名规范
所有资源文件必须使用蛇形命名法（snake_case），且 **Layout 文件** 必须包含以下前缀：
- `activity_*`: Activity 布局
- `fragment_*`: Fragment 布局
- `view_*`: 自定义 View 布局
- `item_*`: 列表项布局
- `dialog_*`: 对话框布局

### 2. 依赖管理
项目强制使用 `gradle/libs.versions.toml` (Version Catalog) 管理所有第三方依赖和插件版本，严禁在 `build.gradle` 中硬编码版本号。

### 3. 异步与存储
- **异步**: 优先使用 **Kotlin Coroutines** 和 **Flow**。
- **存储**: 键值对存储优先使用 **MMKV**。
- **图片**: 统一使用 **Coil 3**。
- **数据库**: 使用 **Room** 并开启 `schemaLocation` 导出功能，以便进行版本追踪和迁移测试。

------------------------------------------------------------------------

# ⚙️ 技术栈

| 技术 | 说明 |
| :--- | :--- |
| **语言** | Kotlin 2.x |
| **UI** | ViewBinding + Jetpack Compose (可选) |
| **异步** | Kotlin Coroutines & Flow |
| **网络** | Retrofit 3 + OkHttp 5 |
| **持久化** | MMKV + Room |
| **依赖注入** | 手动单例 (Manual Singleton) |
| **静态分析** | Lint, Detekt |

------------------------------------------------------------------------

# 🚀 自动路由机制 (Router X)

项目采用基于 **KSP (Kotlin Symbol Processing)** 和 **SPI (Service Provider Interface)** 机制的 **Router X** 路由库，实现极致的解耦与性能：

**主要特性**：
- **编译时扫描**：利用 KSP 在编译期生成注册表，零反射开销。
- **SPI 自动注册**：各模块路由表通过 ServiceLoader 自动发现，无需手动配置模块依赖。
- **分组懒加载**：按需加载路由信息，优化启动速度。
- **自动属性注入**：通过 `@Param` 实现 Intent 参数的自动化赋值。

详细使用说明请参考：[Router X 使用指南](file:///c:/work/AI/ApplicationArchitecture/docs/ROUTER_GUIDE.md)

---

# 🔧 提效工作流 (Workflows)

通过 AI Assistant 提供的快捷命令（Slash Commands）快速完成日常开发任务：

- `/check-rules`: 检查项目是否符合命名规范、资源规则和架构约束。
- `/new-page`: 快速创建新的 Android 页面（含 ViewModel + Layout + 注册）。
- `/code-analysis`: 运行静态代码扫描并生成质量报告。

------------------------------------------------------------------------

# 🎯 架构设计理念

本项目结合了多种现代 Android 架构思想：
- **Modular Architecture**: 通过模块拆分实现并行开发和快速编译。
- **Decoupled by Router**: 物理隔离，逻辑连接，实现真正的业务解耦。
- **Automated Infrastructure**: 通过 Gradle 自动化任务减少人工出错率。

这种设计让项目在代码规模增长时，依然保持良好的可维护性和可扩展性。
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
