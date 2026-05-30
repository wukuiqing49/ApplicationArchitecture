package com.wkq.common.web

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 预加载进程的服务，运行在 :web 进城
 */
class WebProcessService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动后立即停止，仅为了触发进程创建
        stopSelf()
        return START_NOT_STICKY
    }
}
