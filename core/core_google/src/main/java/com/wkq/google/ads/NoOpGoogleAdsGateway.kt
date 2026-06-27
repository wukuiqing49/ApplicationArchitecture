package com.wkq.google.ads

object NoOpGoogleAdsGateway : GoogleAdsGateway {
    override fun isAdsEnabled(): Boolean = false
}
