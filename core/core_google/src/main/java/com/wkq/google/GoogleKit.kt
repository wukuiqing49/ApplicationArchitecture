package com.wkq.google

import android.content.Context
import com.wkq.google.ads.GoogleAdsManager
import com.wkq.google.auth.GoogleAuthManager
import com.wkq.google.billing.GoogleBillingManager
import com.wkq.google.firebase.GoogleFirebaseManager
import com.wkq.google.rate.GoogleRateManager

/**
 * Google 能力统一入口。
 *
 * 外部模块只需要依赖 core_google，并在 Application 中调用 initialize，
 * 后续通过 auth / ads / billing / firebase / rate 访问具体能力。
 */
object GoogleKit {

    @Volatile
    private var config: GoogleKitConfig = GoogleKitConfig()

    /** Google 登录能力入口。 */
    val auth: GoogleAuthManager
        get() = GoogleAuthManager

    /** Google 广告能力入口。 */
    val ads: GoogleAdsManager
        get() = GoogleAdsManager

    /** Google Play Billing 支付能力入口。 */
    val billing: GoogleBillingManager
        get() = GoogleBillingManager

    /** Firebase 能力入口，当前封装 Analytics 常用方法。 */
    val firebase: GoogleFirebaseManager
        get() = GoogleFirebaseManager

    /** 应用评分和反馈引导能力入口。 */
    val rate: GoogleRateManager
        get() = GoogleRateManager

    /**
     * 初始化 Google 工具模块。
     *
     * 建议在 Application.onCreate 中调用一次。该方法会保存配置，并初始化评分、广告、支付和 Firebase 模块。
     */
    fun initialize(context: Context, config: GoogleKitConfig) {
        this.config = config
        GoogleRateManager.initialize(context)
        GoogleAdsManager.initialize(context, config)
        GoogleBillingManager.initialize(context)
        GoogleFirebaseManager.initialize(context, config)
    }

    fun requireConfig(): GoogleKitConfig = config

    internal fun currentConfig(): GoogleKitConfig = config
}
