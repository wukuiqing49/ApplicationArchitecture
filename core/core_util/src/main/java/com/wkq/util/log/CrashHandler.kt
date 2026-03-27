package com.wkq.util.log

import java.io.PrintWriter
import java.io.StringWriter

/**
 * 全局崩溃捕获
 *
 * 功能：
 * 1. 捕获未处理异常
 * 2. 写入日志文件
 * 3. 打印完整堆栈
 * 4. 继续系统崩溃流程
 */
internal class CrashHandler : Thread.UncaughtExceptionHandler {

    /** 系统默认崩溃处理 */
    private val defaultHandler =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(
        t: Thread,
        e: Throwable
    ) {

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)

        // 主异常
        e.printStackTrace(printWriter)

        // 递归 cause
        var cause = e.cause

        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }

        val crash = writer.toString()

        // 写入日志
        ALog.e("Crash", crash)

        // 强制刷新
        ALog.flush()
        ALog.close()

        // 继续系统崩溃
        defaultHandler?.uncaughtException(t, e)
    }
}