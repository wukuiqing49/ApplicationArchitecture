# core_google 接入说明

`core_google` 是项目内的 Google 能力工具模块，目标是让外部业务只依赖一个模块，就能使用 Google 登录、AdMob 广告、Google Play Billing 支付和应用评分反馈能力。

## 一、模块能力

- Google 登录：基于 Credential Manager 和 Google ID Token。
- AdMob 广告：支持 Banner、插屏广告、激励广告。
- Google Play Billing：支持商品查询、拉起购买、查询已购买项目、确认购买。
- Firebase Analytics：支持事件上报、用户 ID、用户属性、采集开关和本地数据清理。
- 评分反馈：支持自动弹窗策略，也支持手动展示评分弹窗。

## 二、依赖方式

在需要使用 Google 能力的模块中添加依赖：

```groovy
implementation project(':core:core_google')
```

依赖版本统一维护在 `gradle/libs.versions.toml`，不要在业务模块中硬编码 Google SDK 版本。

## 三、初始化

建议在 `Application.onCreate()` 中初始化一次：

```kotlin
GoogleKit.initialize(
    context = applicationContext,
    config = GoogleKitConfig(
        serverClientId = "你的 Google Web Client ID",
        enableAds = true,
        bannerAdUnitId = "你的 Banner 广告位 ID",
        interstitialAdUnitId = "你的插屏广告位 ID",
        rewardedAdUnitId = "你的激励广告位 ID",
        billingInAppProductIds = listOf("remove_ads"),
        billingSubscriptionIds = listOf("vip_month"),
        enableFirebaseAnalytics = true,
        appName = "CaptionMeta",
        feedbackEmail = "feedback@example.com"
    )
)
```

`serverClientId` 必须使用 Google Cloud / Firebase 控制台里的 Web Client ID，不是 Android Client ID。

## 四、配置归属表

不是所有 Google 配置都能放进代码里。Android 编译、Manifest 合并和 Google 控制台校验各有自己的职责，建议按下面方式拆分。

| 配置项 | 推荐位置 | 是否可代码配置 | 说明 |
| --- | --- | --- | --- |
| Google SDK 依赖版本 | `gradle/libs.versions.toml` | 否 | 编译期必须确定，不能运行时配置。 |
| `core_google` 模块依赖 | 宿主模块 `build.gradle` | 否 | 外部模块需要显式依赖 `project(':core:core_google')`。 |
| AdMob App ID | Manifest / Gradle placeholder | 不建议 | AdMob SDK 会读取 Manifest meta-data，正式包必须替换真实 App ID。 |
| Banner 广告位 ID | `GoogleKitConfig` | 是 | 业务运行时参数，可由代码或远端配置传入。 |
| 插屏广告位 ID | `GoogleKitConfig` | 是 | 业务运行时参数，可由代码或远端配置传入。 |
| 激励广告位 ID | `GoogleKitConfig` | 是 | 业务运行时参数，可由代码或远端配置传入。 |
| Google Web Client ID | `GoogleKitConfig` | 是 | 登录时传入，但 ID 本身必须先在 Google Cloud / Firebase 控制台创建。 |
| Android 包名 | Google Cloud / Firebase / Play Console | 否 | 由应用构建配置决定，控制台必须配置一致。 |
| SHA-1 / SHA-256 | Google Cloud / Firebase / Play Console | 否 | 由签名证书决定，代码无法替代。 |
| Play Billing 商品 ID | Play Console + `GoogleKitConfig` | 部分是 | 商品必须先在 Play Console 创建，App 内只传对应 productId。 |
| 订阅/内购价格 | Play Console | 否 | 价格、地区、优惠等由 Play Console 管理。 |
| Firebase SDK 依赖 | `core_google` | 否 | 当前模块已接入 Firebase BoM 和 Analytics。 |
| Firebase 项目配置文件 | `app/google-services.json` | 否 | 和宿主 App applicationId 绑定，不能放在 library module 里通用复用。 |
| `google-services` 插件 | 宿主 `app/build.gradle` | 否 | 插件要参与最终 App 的资源生成和配置合并。 |
| Firebase Analytics 开关 | `GoogleKitConfig` | 是 | 可用 `enableFirebaseAnalytics` 控制默认是否启用。 |
| 评分弹窗应用名 | `GoogleKitConfig` | 是 | 为空时模块会读取应用 label。 |
| 反馈邮箱 | `GoogleKitConfig` | 是 | 用于评分较低时拉起邮件反馈。 |

简单理解：

- Gradle：只负责依赖和编译期版本。
- Manifest：只保留 SDK 强制读取的声明，比如 AdMob App ID。
- Google 控制台：负责包名、签名、OAuth、商品、价格等服务端校验信息。
- `GoogleKitConfig`：负责业务运行时参数，比如广告位、商品 ID、邮箱、开关。

Firebase 额外需要宿主 App 完成两件事：

```groovy
plugins {
    alias(libs.plugins.google.services)
}
```

并把 Firebase 控制台下载的配置文件放到：

```text
app/google-services.json
```

## 五、AdMob App ID

模块 Manifest 中已经声明了 AdMob App ID 占位符：

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${googleAdsApplicationId}" />
```

默认值是 Google 测试 App ID。正式发布时，宿主 App 应在自己的 Gradle 配置中覆盖：

```groovy
defaultConfig {
    manifestPlaceholders = [
        googleAdsApplicationId: "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"
    ]
}
```

如果宿主 App 没有覆盖，模块会使用 Google 官方测试 App ID，适合 Debug 调试，不适合正式发布。

## 六、Google 登录

```kotlin
val result = GoogleKit.auth.signIn(activity)
result.onSuccess { account ->
    // account.idToken 可传给后端校验
}.onFailure { error ->
    // error.message 可直接展示给用户
}
```

退出登录状态：

```kotlin
GoogleKit.auth.signOut(context)
```

注意：`signOut` 只清理当前 App 的 Credential 状态，不会移除系统 Google 账号。

## 七、广告

### Banner

```kotlin
GoogleKit.ads.createBannerAdView(activity)?.let { bannerView ->
    container.addView(bannerView)
}
```

### 插屏广告

```kotlin
GoogleKit.ads.loadInterstitial(activity) { result ->
    result.onSuccess { ad ->
        GoogleKit.ads.showInterstitial(
            activity = activity,
            ad = ad,
            onDismissed = {
                // 广告关闭后继续业务流程
            },
            onFailed = { message ->
                // 展示失败兜底
            }
        )
    }
}
```

### 激励广告

```kotlin
GoogleKit.ads.loadRewarded(activity) { result ->
    result.onSuccess { ad ->
        GoogleKit.ads.showRewarded(
            activity = activity,
            ad = ad,
            onReward = { reward ->
                // 在这里发放奖励
            }
        )
    }
}
```

## 八、Google Play Billing

设置购买结果监听：

```kotlin
GoogleKit.billing.setPurchaseUpdatedListener { response, purchases ->
    if (response.isSuccess) {
        purchases.forEach { purchase ->
            // 保存 purchaseToken，并按业务需要确认购买
        }
    }
}
```

查询商品：

```kotlin
val products = GoogleKit.billing.queryProducts(
    productIds = listOf("vip_month"),
    productType = GoogleProductType.SUBS
)
```

拉起购买：

```kotlin
GoogleKit.billing.launchPurchase(
    activity = activity,
    productId = "vip_month"
)
```

确认购买：

```kotlin
GoogleKit.billing.acknowledgePurchase(purchaseToken)
```

生产环境建议把 `purchaseToken` 传给服务端校验，不要只依赖本地查询结果判断会员有效期。

## 九、Firebase Analytics

判断 Analytics 是否可用：

```kotlin
val available = GoogleKit.firebase.isAnalyticsAvailable()
```

上报事件：

```kotlin
GoogleKit.firebase.logEvent(
    name = "template_apply",
    params = mapOf(
        "template_id" to "news",
        "has_location" to true
    )
)
```

设置用户 ID：

```kotlin
GoogleKit.firebase.setUserId(userId)
```

设置用户属性：

```kotlin
GoogleKit.firebase.setUserProperty("vip", "true")
```

隐私协议未同意前可关闭采集：

```kotlin
GoogleKit.firebase.setAnalyticsCollectionEnabled(false)
```

用户注销或撤回授权时可清理本地 Analytics 数据：

```kotlin
GoogleKit.firebase.resetAnalyticsData()
```

如果宿主 App 没有配置 `google-services.json`，Firebase 初始化会失败，`isAnalyticsAvailable()` 会返回 false，可通过下面方法查看 Debug 原因：

```kotlin
GoogleKit.firebase.getLastInitializeErrorMessage()
```

## 十、评分反馈

自动判断并展示：

```kotlin
GoogleKit.rate.showIfNeeded(activity, minDays = 1)
```

用户主动点击时强制展示：

```kotlin
GoogleKit.rate.show(activity)
```

## 十一、注意事项

- 广告正式上线前必须替换真实 AdMob App ID 和广告位 ID。
- Google 登录需要配置正确的 SHA-1 / SHA-256、包名和 Web Client ID。
- 支付商品必须先在 Play Console 创建，并使用和控制台一致的 productId。
- 非消耗型商品和订阅购买成功后必须确认购买。
- Firebase Analytics 正式使用前，宿主 App 必须配置 `google-services.json` 和 `google-services` 插件。
- 当前模块不处理服务端验签，会员有效期建议以后接入后端校验。
