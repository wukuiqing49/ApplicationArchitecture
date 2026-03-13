package com.wkq.util

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings


/**
 *
 *@Author: wkq
 *
 *@Time: 2025/12/11 10:39
 *
 *@Desc:
 */
object NotificationUtil {
    /**
    * 判断是否 开启通知
     */
    fun isNotificationEnabled(context: Context, channelId: String? = null): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelEnabled = channelId?.let {
                manager.getNotificationChannel(it)?.importance != NotificationManager.IMPORTANCE_NONE
            } ?: true
            manager.areNotificationsEnabled() && channelEnabled
        } else {
            manager.areNotificationsEnabled()
        }
    }
    /**
    * 开启通知
     */
    fun openNotificationSettings(mContext: Activity) {
//        PopupUtil.createCommonPopupView(
//            mContext,
//            mContext.resources.getString(R.string.permission_request_setting_title),
//            mContext.resources.getString(R.string.permissions_granted_setting),
//            mContext.resources. getString(R.string.go_to_settings),
//            object : CommonPopupListener {
//                override fun sureClick() {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        openSettingsPage(mContext,Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
//                            putExtra(Settings.EXTRA_APP_PACKAGE,   mContext.packageName)
//                        }
//                    } else {
//                        // 低版本退回应用详情页
//                        openAppDetailsSettings(mContext)
//                    }
//                }
//
//                override fun cancelClick() {}
//            })


    }

    fun openAppDetailsSettings(mContext: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", mContext.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        mContext.startActivity(intent)
    }

    /**
     * 通用方法：跳转系统设置页面
     * @param action 系统设置 action，例如 Settings.ACTION_APP_NOTIFICATION_SETTINGS
     * @param configure 可选 lambda，用于对 Intent 做额外配置（比如传包名）
     */
    fun openSettingsPage(mContext: Context,action: String, configure: (Intent.() -> Unit)? = null) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 保证从非 Activity Context 也能启动
            configure?.invoke(this)
        }
        mContext.startActivity(intent)
    }

}
