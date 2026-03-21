package com.wkq.util

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build

/**
 * 进程与包名相关工具类
 */
object ProcessUtils {

    /**
     * 获取当前进程名
     */
    fun getProcessName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val runningApps = am?.runningAppProcesses ?: return null
        for (processInfo in runningApps) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName
            }
        }
        // 反射兜底方案
        return try {
            val declaredMethod = Class.forName(
                "android.app.ActivityThread",
                false,
                Application::class.java.classLoader
            ).getDeclaredMethod("currentProcessName")
            declaredMethod.isAccessible = true
            declaredMethod.invoke(null) as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断当前是否为主进程
     */
    fun isMainProcess(context: Context): Boolean {
        return context.packageName == getProcessName(context)
    }

    /**
     * 判断是否为指定后缀的子进程
     * @param suffix 进程名后缀，如 ":web"
     */
    fun isSubProcess(context: Context, suffix: String): Boolean {
        val processName = getProcessName(context) ?: return false
        return processName.endsWith(suffix)
    }
}
