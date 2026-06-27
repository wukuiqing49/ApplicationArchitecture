package com.wkq.google.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.wkq.google.GoogleKit
import com.wkq.google.GoogleKitConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob 广告门面。
 *
 * 当前封装 Banner、插屏和激励广告。外部业务不需要直接持有 AdMob 的加载回调。
 */
object GoogleAdsManager : GoogleAdsGateway {

    private val initialized = AtomicBoolean(false)

    /** 返回当前配置是否允许展示广告。 */
    override fun isAdsEnabled(): Boolean {
        return GoogleKit.currentConfig().enableAds
    }

    /**
     * 初始化 Mobile Ads SDK。
     *
     * enableAds 为 false 时不会初始化，避免无广告业务的 App 产生额外启动成本。
     */
    fun initialize(context: Context, config: GoogleKitConfig = GoogleKit.currentConfig()) {
        if (!config.enableAds || !initialized.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext)
    }

    /**
     * 创建并加载 Banner 广告 View。
     *
     * @return 广告未启用或广告位为空时返回 null。
     */
    fun createBannerAdView(
        context: Context,
        adUnitId: String = GoogleKit.currentConfig().bannerAdUnitId
    ): View? {
        if (!isAdsEnabled() || adUnitId.isBlank()) return null
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * 加载插屏广告。
     *
     * 加载成功后返回 GoogleInterstitialAd，调用方可在合适的页面节点调用 showInterstitial 展示。
     */
    fun loadInterstitial(
        context: Context,
        adUnitId: String = GoogleKit.currentConfig().interstitialAdUnitId,
        callback: (Result<GoogleInterstitialAd>) -> Unit
    ) {
        if (!isAdsEnabled() || adUnitId.isBlank()) {
            callback(Result.failure(IllegalStateException("Interstitial ads are disabled or the ad unit ID is empty.")))
            return
        }
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    callback(Result.success(GoogleInterstitialAd(ad)))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    callback(Result.failure(IllegalStateException(error.message)))
                }
            }
        )
    }

    /**
     * 展示已加载的插屏广告。
     *
     * @param onDismissed 广告关闭回调。
     * @param onFailed 展示失败回调，返回失败原因。
     */
    fun showInterstitial(
        activity: Activity,
        ad: GoogleInterstitialAd,
        onDismissed: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        ad.source.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onFailed(error.message)
            }
        }
        ad.source.show(activity)
    }

    /**
     * 加载激励广告。
     *
     * 加载成功后返回 GoogleRewardedAd，调用方展示后在 onReward 中发放奖励。
     */
    fun loadRewarded(
        context: Context,
        adUnitId: String = GoogleKit.currentConfig().rewardedAdUnitId,
        callback: (Result<GoogleRewardedAd>) -> Unit
    ) {
        if (!isAdsEnabled() || adUnitId.isBlank()) {
            callback(Result.failure(IllegalStateException("Rewarded ads are disabled or the ad unit ID is empty.")))
            return
        }
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    callback(Result.success(GoogleRewardedAd(ad)))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    callback(Result.failure(IllegalStateException(error.message)))
                }
            }
        )
    }

    /**
     * 展示已加载的激励广告。
     *
     * @param onReward 用户达到激励条件时回调，业务可在这里发放权益。
     * @param onDismissed 广告关闭回调。
     * @param onFailed 展示失败回调，返回失败原因。
     */
    fun showRewarded(
        activity: Activity,
        ad: GoogleRewardedAd,
        onReward: (GoogleReward) -> Unit,
        onDismissed: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        ad.source.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onFailed(error.message)
            }
        }
        ad.source.show(activity) { reward ->
            onReward(GoogleReward(amount = reward.amount, type = reward.type))
        }
    }
}
