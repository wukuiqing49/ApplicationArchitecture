package com.wkq.base.util

/**
 * 统一付费与 Pro 订阅状态校验管理器
 * 作为核心底层哨兵服务，解耦业务支付逻辑
 */
object VipManager {
    private var isSubscribed = false

    /**
     * 更新全局订阅状态
     * @param subscribed true 代表已订阅专业版 Pro，false 代表未订阅/已失效
     */
    fun updateSubscriptionStatus(subscribed: Boolean) {
        isSubscribed = subscribed
    }

    /**
     * 判断当前用户是否拥有专业版 Pro 权益
     */
    fun isPro(): Boolean {
        return isSubscribed
    }
}
