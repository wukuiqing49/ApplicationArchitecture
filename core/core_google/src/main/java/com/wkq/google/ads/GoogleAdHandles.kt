package com.wkq.google.ads

import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd

class GoogleInterstitialAd internal constructor(
    internal val source: InterstitialAd
)

class GoogleRewardedAd internal constructor(
    internal val source: RewardedAd
)
