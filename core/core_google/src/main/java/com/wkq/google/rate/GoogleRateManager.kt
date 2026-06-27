package com.wkq.google.rate

import android.app.Activity
import android.content.Context
import com.wkq.google.GoogleKit

/**
 * 应用评分和反馈引导门面。
 *
 * 统一封装自动弹出策略、手动展示评分弹窗以及已评分状态。
 */
object GoogleRateManager {

    /** 初始化首次使用时间，用于后续判断是否满足自动弹窗条件。 */
    fun initialize(context: Context) {
        RateAppController.initFirstUseIfNeeded(context.applicationContext)
    }

    /**
     * 判断当前是否应该自动展示评分弹窗。
     *
     * @param minDays 首次使用后至少经过多少天才允许自动展示。
     */
    fun shouldShow(context: Context, minDays: Int = 1): Boolean {
        return RateAppController.shouldShowRateDialog(context, minDays)
    }

    /**
     * 满足策略时自动展示评分弹窗。
     *
     * @return true 表示本次已展示弹窗，false 表示不满足展示条件。
     */
    fun showIfNeeded(
        activity: Activity,
        minDays: Int = 1,
        appName: String = GoogleKit.currentConfig().appName,
        feedbackEmail: String = GoogleKit.currentConfig().feedbackEmail,
        onDismiss: (() -> Unit)? = null
    ): Boolean {
        if (!shouldShow(activity, minDays)) return false
        show(activity, appName, feedbackEmail, force = false, onDismiss = onDismiss)
        RateAppController.increasePromptCount(activity)
        return true
    }

    /**
     * 立即展示评分弹窗。
     *
     * 常用于“关于我们”“设置”等用户主动点击评分入口的场景。
     */
    fun show(
        activity: Activity,
        appName: String = GoogleKit.currentConfig().appName,
        feedbackEmail: String = GoogleKit.currentConfig().feedbackEmail,
        force: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ) {
        val resolvedAppName = appName.ifBlank {
            activity.applicationInfo.loadLabel(activity.packageManager).toString()
        }
        RateAppDialogHelper.showRateDialog(
            activity = activity,
            appName = resolvedAppName,
            feedbackEmail = feedbackEmail,
            force = force,
            onDismiss = onDismiss
        )
    }

    /** 标记用户已评分或已反馈，后续自动弹窗不再重复打扰。 */
    fun markRated(context: Context) {
        RateAppController.markRated(context)
    }

    /** 判断用户是否已经评分或反馈。 */
    fun isRated(context: Context): Boolean {
        return RateAppController.isRated(context)
    }
}
