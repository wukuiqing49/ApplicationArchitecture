---
description: 创建新的 Android Activity 页面（含 ViewModel + Layout + 注册）
---

根据你的项目规则（MVVM + ViewBinding + 资源命名规范），按照以下步骤创建一个完整的新页面。

**使用方式**：告诉我页面名称，我会自动创建所有文件。例如：
> "按照 new-page workflow 创建一个用户设置页（Settings）"

### Step 1: 创建 Layout 文件
- 命名格式：`activity_<name>.xml`
- 使用 Material Design 组件
- 所有 View ID 使用 `snake_case`（如 `btn_submit`、`tv_title`）
- **不允许**硬编码字符串

### Step 2: 创建 ViewModel
- 路径：`app/src/main/java/com/example/framework/<name>/`
- 命名：`<Name>ViewModel.kt`
- 继承 `ViewModel()`
- 使用 `LiveData` 暴露 UI 状态
- 异步操作使用 `viewModelScope.launch`（Coroutines）

### Step 3: 创建 Activity
- 命名：`<Name>Activity.kt`
- 继承 `BaseActivity<Activity<Name>Binding, <Name>ViewModel>()`
- 实现 `initView()` 和 `initData()` 两个抽象方法
- 在 `initData()` 中 observe LiveData

### Step 4: 添加字符串资源
- 文件：`app/src/main/res/values/strings.xml`
- 所有字符串使用功能前缀，如 `<name>_title`、`<name>_btn_submit`

### Step 5: 注册到 AndroidManifest.xml
- 在 `<application>` 内添加 `<activity android:name=".<name>.<Name>Activity" />`
