package com.wkq.iptc.camera.camera

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 拍照任务线程管理器。
 *
 * CameraX 图片保存回调不应阻塞主线程，因此使用单线程 Executor 串行处理拍照任务。
 */
class CaptureExecutor {
    /** 拍照文件写入线程。 */
    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * 关闭拍照线程。
     *
     * 页面销毁时调用，避免线程泄漏。
     */
    fun shutdown() {
        executor.shutdown()
        if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
            executor.shutdownNow()
        }
    }
}
