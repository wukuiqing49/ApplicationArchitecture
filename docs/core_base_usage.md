# core_base 内部使用说明

`core_base` 是项目的基础框架模块，负责沉淀通用 `Activity`、`Fragment`、`ViewModel`、列表、Adapter、弹框、权限、空布局、标题栏和系统栏适配能力。业务模块优先继承或复用这里的基础能力，不要在各页面重复实现同一套代码。

## 结论

- 本工程内部页面优先依赖 `:core:core_base`，普通业务页面不需要再单独引入标题栏、权限、弹框、列表基类等重复能力。
- `core_base` 当前适合做“页面基础层”，不是业务数据层，也不是复杂 UI 组件库。
- 内部 release/R8 正常使用默认支持混淆，不需要业务模块额外写专属混淆配置。
- 新增基础能力时必须同时补 demo 和本文档，否则其他模块很难知道该怎么用。

## 模块定位

- `core_base` 放基础结构：`BaseActivity`、`BaseTitleActivity`、`BaseListActivity`、`BaseFragment`、Adapter、Insets、权限、弹框、空布局、标题栏。
- `core_ui` 放纯 UI 组件和 UI helper，例如复杂自定义 View、图片圆角样式、Tab/Indicator 相关封装。
- `feature_test` 只放 demo 和测试入口，不承载可复用基础封装。

## 内部接入方式

本工程内模块直接依赖：

```gradle
implementation project(':core:core_base')
```

`core_base` 内部已经依赖：

- AppCompat / Material
- Lifecycle ViewModel / Runtime
- Activity KTX / Fragment KTX
- ConstraintLayout / RecyclerView / ViewPager2
- SmartRefreshLayout
- XPopup
- Toasty

业务模块通常只需要依赖 `core_base`。只有业务代码自己直接使用三方库类型、资源或扩展 API 时，才需要在业务模块单独声明对应依赖。

## GitHub 网络引用

`core_base` 已支持发布到 GitHub 后通过 JitPack 或 GitHub Packages 网络引用。推荐公开库优先使用 JitPack，使用方不需要配置 token。

详细发布和引用方式见：

```text
docs/core_base_publish.md
```

## 基类选择

| 场景 | 推荐基类 |
| --- | --- |
| 普通页面，无统一标题栏 | `BaseActivity` |
| 普通页面 + ViewModel | `BaseVMActivity` |
| 带通用标题栏页面 | `BaseTitleActivity` |
| 带通用标题栏页面 + ViewModel | `BaseVMTitleActivity` |
| 标准分页列表页 | `BaseListActivity` |
| 标准分页列表页 + ViewModel | `BaseVMListActivity` |
| 视频、相机、播放器、游戏等真全屏页 | `BaseFullScreenActivity` |
| Fragment 普通内容 | `BaseFragment` / `BaseVMFragment` |
| Fragment 标准分页列表 | `BaseListFragment` / `BaseVMListFragment` |

内部开发原则：能用已有基类就不要复制一套页面结构；确实不适合时再扩展基类。

## BaseViewModel

`BaseViewModel` 是统一 ViewModel 父类，目前内置：

```kotlin
val errorMutableLiveData = MutableLiveData<String>()
val uiStateLiveData: LiveData<BaseUiState>
val uiEventLiveData: LiveData<ConsumableEvent<BaseUiEvent>>
```

`BaseUiState` 包含：

- `Idle`
- `Loading(message)`
- `Content`
- `Empty(message)`
- `Error(message, throwable)`

ViewModel 内部可使用：

```kotlin
class DemoViewModel : BaseViewModel() {
    fun load() {
        showLoading("加载中")
        // success
        showContent()
        // empty
        showEmpty("暂无数据")
        // error
        showError("加载失败")
    }
}
```

一次性事件用于 Toast、弹框、返回、导航等动作：

```kotlin
class DemoViewModel : BaseViewModel() {
    fun submit() {
        sendToast("保存成功")
        sendConfirmDialog("delete_user", "提示", "确认删除？")
        sendFinish()
        sendNavigate("/test/detail")
    }

    override fun onConfirmDialogResult(requestKey: String, result: ConfirmDialogResult) {
        if (requestKey == "delete_user" && result == ConfirmDialogResult.CONFIRM) {
            deleteUser()
        }
    }
}
```

`BaseUiEvent` 包含：

- `Toast(message)`：默认自动 Toast。
- `ConfirmDialog(requestKey, title, message)`：默认自动显示确认弹框，确认/取消结果会回调到 `BaseViewModel.onConfirmDialogResult(requestKey, result)`。
- `Finish`：Activity 默认 `finish()`。
- `Navigate(path)`：默认不处理，页面可重写 `onBaseUiEvent()` 接入项目路由。

建议：

- 页面通用错误提示兼容保留 `errorMutableLiveData`。
- 新页面优先观察 `uiStateLiveData`，统一处理 loading、content、empty、error。
- `BaseVMActivity`、`BaseVMTitleActivity`、`BaseVMListActivity`、`BaseVMFragment`、`BaseVMListFragment` 会在 `VM : BaseViewModel` 时自动观察 `uiStateLiveData`。
- 默认 `Loading` 显示 `LoadingDialog`，`Error` 显示 Toast，列表页 `Empty/Content` 会自动切换空布局和内容布局。
- 如页面不想使用默认处理，可重写 `enableBaseUiStateObserver()` 返回 `false`；如需扩展，可重写 `onBaseUiStateChanged()`、`onBaseUiEmpty()`、`onBaseUiContent()`。
- 如需接管一次性事件，可重写 `onBaseUiEvent(event)`，返回 `true` 表示页面已处理，基类不再执行默认逻辑。
- `ViewModel` 不要持有 `Activity`、`Fragment`、`View` 或长生命周期 `Context`。
- `ConfirmDialog` 推荐重写 `onConfirmDialogResult(requestKey, result: ConfirmDialogResult)`；旧的 `Boolean` 回调仍保留，用于兼容已有页面。

## Activity 基类

### BaseActivity

适合没有通用标题栏的普通页面。

能力：

- 自动通过泛型反射创建 ViewBinding。
- 默认开启 edge-to-edge。
- 默认处理状态栏、导航栏和手势区域 Insets。
- 集成权限申请能力。

示例：

```kotlin
class DemoActivity : BaseActivity<ActivityDemoBinding>() {

    override fun initView() {
        binding.tvTitle.text = "Demo"
    }

    override fun initData() {
        // 加载数据
    }
}
```

注意：

- 泛型必须是具体 `ViewBinding` 类型。
- 不要在子类重复 `setContentView()`。
- 如页面需要特殊系统栏策略，可重写 `applyDefaultSystemBarsInsets()` 或 `setStatusBarDarkFont()`。
- 不建议中间再包多层泛型基类；如果必须封装，要同步验证 ViewBinding 泛型解析是否还能拿到具体类型。

### BaseVMActivity

适合普通页面 + ViewModel。

```kotlin
class DemoVMActivity : BaseVMActivity<ActivityDemoBinding, DemoViewModel>() {

    override fun initView() {
        viewModel.state.observe(this) {
            binding.tvTitle.text = it
        }
    }

    override fun initData() {
        viewModel.load()
    }
}
```

## 标题页

### BaseTitleActivity

适合带 `CommonTitleBar` 的普通标题页。

能力：

- 自动创建标题栏容器和内容容器。
- `contentBinding` 是业务内容布局。
- 标题栏默认处理状态栏 Insets，实现沉浸式标题栏。
- 左侧按钮默认 `finish()`。

示例：

```kotlin
class DemoTitleActivity : BaseTitleActivity<ActivityDemoContentBinding>() {

    override fun initView() {
        setPageTitle("标题页")
        setRightText("保存") {
            // 保存
        }

        contentBinding.tvContent.text = "内容"
    }

    override fun initData() = Unit
}
```

常用方法：

- `setPageTitle(title)`
- `setRightText(text) { }`
- `setRightIcon(resId) { }`
- `setLeftIcon(resId) { }`
- `setLeftClickListener { }`
- `setContentBelowTitleBar()`
- `setContentOverlapTitleBar(true)`
- `setViewBelowTitleBar(view)`

注意：

- 标题页不要再手动给根布局加状态栏高度。
- 如果内容要铺到标题栏下面，使用 `setContentOverlapTitleBar(true)`。
- `setBackgroundColor()` 会覆盖 shape 背景，修改标题栏背景时要谨慎。
- 返回按钮默认走 `CommonTitleBar` 左侧区域，`BaseTitleActivity` 已默认处理 `finish()`。

### BaseVMTitleActivity

适合标题页 + ViewModel。

```kotlin
class DemoVMTitleActivity :
    BaseVMTitleActivity<ActivityDemoContentBinding, DemoViewModel>() {

    override fun initView() {
        setPageTitle("VM 标题页")
        viewModel.state.observe(this) {
            contentBinding.tvContent.text = it
        }
    }

    override fun initData() {
        viewModel.load()
    }
}
```

## 列表页

### BaseListActivity

适合标准分页列表页面，内置：

- `SmartRefreshLayout`
- `RecyclerView`
- `EmptyView`
- 顶部 header 容器
- 底部 footer 容器
- 下拉刷新、上拉加载更多
- 空布局显示
- 底部导航栏/手势区域适配

示例：

```kotlin
class DemoListActivity : BaseListActivity<DemoItem>() {

    override fun initView() {
        super.initView()
        setHeaderView(CommonTitleBar(this).apply {
            setTitle("列表页")
            setLeftIcon(com.wkq.base.R.mipmap.ic_toolbar_back_black)
            onLeftClickListener = { finish() }
        })
    }

    override fun createAdapter(): BaseRecyclerViewAdapter<*, DemoItem> {
        return DemoAdapter(this)
    }

    override fun loadListData(page: Int) {
        // 请求完成后调用
        finishLoad(data, hasMore)
    }
}
```

刷新完成调用：

```kotlin
finishLoad(data = list, hasMore = hasNextPage)
```

请求失败调用：

```kotlin
finishLoadFailed()
```

常用扩展方法：

- `autoRefreshList()`
- `setEmptyText(text)`
- `showEmptyView(text)`
- `showContentView()`
- `stopRefreshAndLoadMore(success)`

注意：

- 子类 `initView()` 中如果重写，必须先调用 `super.initView()`。
- 不要直接操作刷新完成状态，优先走 `finishLoad()`。
- 不要直接调用 `SmartRefreshLayout`，优先使用基类暴露的方法。
- `setHeaderView(CommonTitleBar)` 会自动处理标题栏状态栏 Insets。
- 如需网格布局，重写 `getLayoutManager()`。

### BaseVMListActivity

适合列表页 + ViewModel。

```kotlin
class DemoVMListActivity : BaseVMListActivity<DemoViewModel, DemoItem>() {
    override fun createAdapter() = DemoAdapter(this)

    override fun loadListData(page: Int) {
        viewModel.loadPage(page)
    }
}
```

## 全屏页

### BaseFullScreenActivity

适合视频、相机、播放器、游戏等真正全屏页面。

能力：

- 隐藏状态栏和导航栏。
- 内容铺满屏幕。
- 支持刘海屏短边布局。

注意：

- 全屏页不会默认给根布局加系统栏 padding。
- 底部按钮、浮层等关键控件需要按需调用 `SystemBarInsets.applyBottomInset()` 或 `applyHorizontalGestureInset()`。

### BaseVMFullScreenActivity

适合全屏页 + ViewModel。

## Fragment 基类

### BaseFragment

能力：

- 自动创建 ViewBinding。
- `onDestroyView()` 自动释放 `_binding`。
- 提供 `initViewModel()`、`initView()`、`initData()` 生命周期入口。

```kotlin
class DemoFragment : BaseFragment<FragmentDemoBinding>() {
    override fun initView() {
        binding.tvTitle.text = "Fragment"
    }

    override fun initData() = Unit
}
```

### BaseVMFragment

适合 Fragment + ViewModel。

### BaseListFragment / BaseVMListFragment

能力和 `BaseListActivity` 类似，适合放在 Tab、ViewPager2 或页面局部容器内。

注意：

- Fragment 列表默认不会处理 Activity 顶部标题栏 Insets。
- 宿主 Activity 负责页面级状态栏/导航栏适配。

## Adapter

### BaseRecyclerViewAdapter

使用 ViewBinding 的普通 RecyclerView Adapter。

```kotlin
class DemoAdapter(context: Context) :
    BaseRecyclerViewAdapter<ItemDemoBinding, DemoItem>(
        context,
        ItemDemoBinding::inflate
    ) {
    override fun convert(binding: ItemDemoBinding, item: DemoItem, position: Int) {
        binding.tvTitle.text = item.title
    }
}
```

常用方法：

- `setData(list)`
- `addData(list)`
- `addData(item)`
- `updateData(position, item)`
- `removeAt(position)`
- `clearData()`
- `getData()`

### BaseMultiItemRecyclerViewAdapter

适合多布局列表，数据模型实现 `IBaseMultiItem` 后使用。

```kotlin
private const val TYPE_TITLE = 1
private const val TYPE_CONTENT = 2

data class DemoMultiItem(
    override val itemType: Int,
    val text: String
) : IBaseMultiItem

class DemoMultiAdapter :
    BaseMultiItemRecyclerViewAdapter<DemoMultiItem>() {

    override fun onCreateMultiViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ViewBinding> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TITLE -> BaseViewHolder(ItemTitleBinding.inflate(inflater, parent, false))
            else -> BaseViewHolder(ItemContentBinding.inflate(inflater, parent, false))
        }
    }

    override fun convert(binding: ViewBinding, item: DemoMultiItem, position: Int) {
        when (binding) {
            is ItemTitleBinding -> binding.tvTitle.text = item.text
            is ItemContentBinding -> binding.tvContent.text = item.text
        }
    }
}
```

注意：

- `itemType` 必须稳定，不要用 position 当类型。
- 多布局只负责展示差异，复杂业务判断放到 ViewModel 或页面层。
- 支持 `setData(list)`、`addData(list)`、`addData(item)`、`updateData(position, item)`、`removeAt(position)`、`clearData()`。

### BaseFragmentStateAdapter

适合 `ViewPager2 + Fragment`。

```kotlin
val adapter = BaseFragmentStateAdapter(this)
adapter.setFragments(listOf(fragment1, fragment2))
viewPager.adapter = adapter
```

注意：如果后续项目提供统一 Tab 封装，业务页面优先使用统一封装，不要重复写联动逻辑。

## 通用 View

### CommonTitleBar

支持：

- 标题文字、颜色、大小、样式。
- 左侧图标/文字。
- 右侧图标/文字。
- 点击回调。
- `applyStatusBarInset()` 状态栏适配。

XML 中可使用：

```xml
<com.wkq.base.widget.CommonTitleBar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:titleBar_title="标题"
    app:titleBar_leftIconVisible="true" />
```

常用属性：

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `titleBar_title` | string | 标题文字 |
| `titleBar_titleColor` | color | 标题颜色 |
| `titleBar_titleSize` | dimension | 标题字号 |
| `titleBar_titleStyle` | enum | `normal` / `bold` / `italic` |
| `titleBar_fitStatusBar` | boolean | 是否自动增加状态栏高度 |
| `titleBar_leftIcon` | reference | 左侧图标 |
| `titleBar_leftIconVisible` | boolean | 是否显示左侧图标 |
| `titleBar_leftText` | string | 左侧文字 |
| `titleBar_leftTextColor` | color | 左侧文字颜色 |
| `titleBar_leftTextSize` | dimension | 左侧文字字号 |
| `titleBar_rightIcon` | reference | 右侧图标 |
| `titleBar_rightIconVisible` | boolean | 是否显示右侧图标 |
| `titleBar_rightText` | string | 右侧文字 |
| `titleBar_rightTextColor` | color | 右侧文字颜色 |
| `titleBar_rightTextSize` | dimension | 右侧文字字号 |

注意：

- `CommonTitleBar` 默认 `fitStatusBar=false`。
- 在 `BaseTitleActivity` 中会自动处理状态栏 Insets。
- 普通页面单独使用时，如需要沉浸式标题栏，可设置 `app:titleBar_fitStatusBar="true"` 或调用 `applyStatusBarInset()`。

### EmptyView

用于空布局展示，支持空文案和点击重试。

XML：

```xml
<com.wkq.base.widget.EmptyView
    android:id="@+id/emptyView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:emptyImage="@mipmap/ic_empty"
    app:emptyText="@string/base_empty_no_data"
    app:emptyTextColor="#667085"
    app:emptyTextSize="14sp" />
```

Kotlin：

```kotlin
binding.emptyView.setEmptyText(getString(R.string.base_empty_no_data))
binding.emptyView.setOnEmptyClickListener {
    loadData()
}
```

属性：

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `emptyImage` | reference | 空状态图片 |
| `emptyText` | string | 空状态文案 |
| `emptyTextSize` | dimension | 文案大小 |
| `emptyTextColor` | color | 文案颜色 |

### MultiSpanTextView

适合协议文案、关键词高亮、可点击片段。

```kotlin
binding.multiSpanText.setTextWithSpans(
    "我已阅读《用户协议》和《隐私政策》",
    MultiSpanTextView.SpanItem("《用户协议》", Color.BLUE) {
        openUserAgreement()
    },
    MultiSpanTextView.SpanItem("《隐私政策》", Color.BLUE) {
        openPrivacyPolicy()
    }
)
```

注意：

- `keyword` 会匹配文本中所有相同片段。
- 重叠片段会优先选择靠前且更长的片段。
- 点击高亮默认透明，不会出现系统默认色块。

### VerifyCodeInputView

适合短信验证码、邮箱验证码、一次性口令输入。

XML：

```xml
<com.wkq.base.widget.VerifyCodeInputView
    android:id="@+id/verifyCode"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:verifyCode_length="6"
    app:verifyCode_cellWidth="44dp"
    app:verifyCode_cellHeight="52dp"
    app:verifyCode_cellSpacing="10dp"
    app:verifyCode_numericOnly="true" />
```

Kotlin：

```kotlin
binding.verifyCode.onCodeChangedListener = { code, complete ->
    binding.btnSubmit.isEnabled = complete
}

binding.verifyCode.onCodeCompleteListener = { code ->
    submitCode(code)
}

binding.verifyCode.requestInputFocus()
```

常用方法：

- `requestInputFocus()`
- `clearInputFocus()`
- `getCode()`
- `isComplete()`
- `setCode(code)`
- `clearCode()`
- `setCodeLength(length)`
- `setNumericOnly(enabled)`

常用属性：

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `verifyCode_length` | integer | 验证码位数 |
| `verifyCode_cellWidth` | dimension | 单个格子宽度 |
| `verifyCode_cellHeight` | dimension | 单个格子高度 |
| `verifyCode_cellSpacing` | dimension | 格子间距 |
| `verifyCode_cornerRadius` | dimension | 格子圆角 |
| `verifyCode_borderWidth` | dimension | 边框宽度 |
| `verifyCode_textColor` | color | 输入文字颜色 |
| `verifyCode_textSize` | dimension | 输入文字大小 |
| `verifyCode_normalBorderColor` | color | 默认边框颜色 |
| `verifyCode_focusedBorderColor` | color | 焦点边框颜色 |
| `verifyCode_filledBorderColor` | color | 已输入边框颜色 |
| `verifyCode_normalBgColor` | color | 默认背景色 |
| `verifyCode_focusedBgColor` | color | 焦点背景色 |
| `verifyCode_filledBgColor` | color | 已输入背景色 |
| `verifyCode_numericOnly` | boolean | 是否只允许数字 |

## 弹框

### CommonDialog

确认弹框：

```kotlin
val handle = CommonDialog.showConfirm(
    context = this,
    title = "提示",
    message = "确认执行操作？",
    onConfirm = {
        // 确认
    }
)
```

自定义内容弹框：

```kotlin
CommonDialog.showContent(
    context = this,
    title = "标题",
    contentView = customView,
    onConfirm = {
        true // 返回 true 自动关闭，false 保持弹框
    }
)
```

原始居中弹框：

```kotlin
CommonDialog.showRawCenter(this, customView)
```

### LoadingDialog

```kotlin
val loading = LoadingDialog.show(this, "加载中")
loading.dismiss()
```

注意：

- 弹框返回 `PopupHandle`，页面销毁或任务取消时应主动 `dismiss()`。
- 不要在 Activity/Fragment 销毁后继续弹窗或回调 UI。

## 权限

所有 `BaseActivity` 子类都继承自 `PermissionsActivity`，可直接使用权限能力。

```kotlin
if (!isGranted(getMediaPermissions())) {
    requestAppPermissions(1, getMediaPermissions())
}

override fun authorized(permissionType: Int, permissionList: MutableList<String>) {
    // 权限通过
}
```

常用方法：

- `requestAppPermissions(type, permissions)`
- `isGranted(permissions)`
- `getMediaPermissions()`
- `openAppDetailsSettings()`
- `openNotificationSettings()`

注意：

- `getMediaPermissions()` 已兼容 Android 13/14 媒体权限。
- 权限永久拒绝时会弹窗引导到应用设置页。
- `requestAppPermissions()` 只负责运行时申请，业务模块仍然必须在自己的 `AndroidManifest.xml` 声明对应权限。
- 权限弹框文案走 `core_base` 多语言资源，新增权限提示时不要硬编码中文。

## 系统栏 Insets

`SystemBarInsets` 用于状态栏、导航栏、手势区适配。

常用方法：

- `applySystemBarsInset(view)`
- `applyTopInset(view, resizeHeight = true)`
- `applyBottomInset(view, includeIme = false, extraBottom = 0)`
- `applyScrollableBottomInset(view, extraBottom = 0)`
- `applyBottomMarginInset(view)`
- `applyHorizontalGestureInset(view)`

使用建议：

- 普通 Activity 默认已处理根布局 Insets。
- 标题页默认由 `CommonTitleBar` 处理顶部 Insets。
- 列表页默认处理底部导航栏和手势区域。
- 全屏页关键控件需要按需手动处理。
- 不要使用 `status_bar_height` 这类硬编码高度。

## 国际化

`core_base` 已提供多语言资源，包含：

- 默认 `values`
- `values-en`
- `values-zh`
- `values-zh-rCN`
- `values-zh-rTW`
- `values-ja`
- `values-ko`
- `values-de`
- `values-es`
- `values-fr`
- `values-it`
- `values-pt`
- `values-ru`

新增文案必须放入 `strings.xml`，不要在代码或布局中硬编码用户可见文本。

## 混淆与发布

- `core_base` 已声明 `consumerProguardFiles "consumer-rules.pro"`。
- 本工程内部开启 release/R8 后，按当前代码正常使用默认支持，不需要每个业务模块再写一份专属混淆配置。
- 当前 ViewBinding 泛型解析依赖反射调用 `inflate()`，`consumer-rules.pro` 已保留 ViewBinding 相关规则。
- 如果后续新增 public API 反射入口、WebView JSBridge、序列化模型、注解扫描或三方库特殊规则，应同步写入 `consumer-rules.pro`。
- 不建议业务页面再封装多层泛型基类；这类写法最容易影响 ViewBinding / ViewModel 泛型解析，必须单独打 release 包验证。

## Demo 位置

`feature_test` 中包含 `core_base` 测试入口和示例页面：

- `feature/feature_test/src/main/java/com/wkq/test/CoreBaseDemoActivity.kt`
- `feature/feature_test/src/main/java/com/wkq/test/corebase/CoreBaseActivityDemos.kt`
- `feature/feature_test/src/main/java/com/wkq/test/corebase/CoreBaseFragmentDemos.kt`
- `feature/feature_test/src/main/java/com/wkq/test/corebase/CoreBaseDemoViewModel.kt`
- `feature/feature_test/src/main/java/com/wkq/test/corebase/CoreBaseSampleModels.kt`
- `feature/feature_test/src/main/java/com/wkq/test/MultiSpanTextViewTestActivity.kt`

新增基础能力时，应同步补 demo，确保其他项目接入时能直接看到用法。

## 使用原则

- 优先继承基类，不要复制基类逻辑到业务页面。
- 业务页面只组合能力，不沉淀基础工具。
- 通用能力先放到 `core_base` 或 `core_ui`，再由业务模块使用。
- 修改 public API 前先确认兼容性。
- 新增三方依赖前先说明必要性，并统一放入 `gradle/libs.versions.toml`。
