package com.wkq.google.auth

import android.app.Activity
import android.content.Context
import com.wkq.google.GoogleKit
import com.wkq.google.model.GoogleAccountPayload

/**
 * Google 登录门面。
 *
 * 对外隐藏 Credential Manager 的细节，调用方只需要处理 Result。
 */
object GoogleAuthManager {

    /**
     * 拉起 Google 登录并返回账号信息。
     *
     * @param activity 当前前台 Activity。
     * @param serverClientId Google Web Client ID，默认读取 GoogleKitConfig。
     */
    suspend fun signIn(
        activity: Activity,
        serverClientId: String = GoogleKit.currentConfig().serverClientId
    ): Result<GoogleAccountPayload> {
        return GoogleSignInCoordinator.signIn(activity, serverClientId)
    }

    /**
     * 清理本地 Credential Manager 登录状态。
     *
     * 注意：这不会注销用户的系统 Google 账号，只会清理当前 App 的凭据状态。
     */
    suspend fun signOut(context: Context) {
        GoogleSignInCoordinator.signOut(context)
    }

    /**
     * 构建后端可能需要的登录扩展字段 JSON。
     *
     * @param payload Google 登录返回的账号信息。
     * @param subscriptionStatus 当前订阅状态标识。
     */
    fun buildExtraJson(payload: GoogleAccountPayload, subscriptionStatus: String): String {
        return GoogleSignInCoordinator.buildExtraJson(payload, subscriptionStatus)
    }
}
