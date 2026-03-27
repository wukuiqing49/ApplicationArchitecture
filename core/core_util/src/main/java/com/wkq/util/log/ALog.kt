package com.wkq.util.log

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 *  @ Author: wkq
 *
 *  @ Time: 2026/3/26 9:45
 *
 *
 * 企业级日志工具
 *
 * 功能：
 * 1. 控制台日志
 * 2. 文件日志
 * 3. 按天切割日志
 * 4. 按大小切割日志
 * 5. 崩溃日志自动记录
 * 6. logcat 自动保存
 * 7. Tag 过滤
 * 8. 自动删除过期日志
 * 9. 异步写入（不阻塞UI）
 * 10. Debug/Release 自动控制
 *
 * 日志目录：
 * /files/logs/
 *
 * 文件示例：
 * app-2026-03-26.log
 * app-2026-03-26-1.log
 * logcat-2026-03-26.log
 */
object ALog {

    /** 内部TAG */
    private const val TAG = "ALog"

    /** 是否打印调用栈信息（文件名 行号 方法） */
    private var showStackInfo = true

    /** 是否打印到控制台 */
    private var enableConsole = true

    /** 是否写入文件 */
    private var enableFile = false

    /** 日志文件名前缀 */
    private var logFilePrefix = "app"

    /** 单个日志文件最大大小（默认10MB） */
    private var maxFileSize = 10 * 1024 * 1024L

    /** 日志缓存天数 */
    private var cacheDays = 7

    /** 日志目录 */
    private lateinit var logDir: File

    /** 当前日志文件 */
    private var currentLogFile: File? = null

    /** 文件写入器 */
    private var fileWriter: FileWriter? = null

    /** 是否已初始化 */
    private val initialized = AtomicBoolean(false)

    /** 单线程线程池（保证顺序写入） */
    private val executor = Executors.newSingleThreadExecutor()

    /** 日志时间格式 */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /** 文件名日期格式 */
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** tag 白名单 */
    private val enableTags = mutableSetOf<String>()

    /** 是否启用tag过滤 */
    private var useTagFilter = false

    /**
     * 初始化日志系统
     *
     * @param context application context
     * @param debug 是否打印调试信息
     * @param enableFile 是否写入文件
     * @param logFilePrefix 文件前缀
     * @param maxFileSize 单文件最大大小
     * @param cacheDays 缓存天数
     */
    fun init(
        context: Context, isShow: Boolean = true,showStackInfo: Boolean = true, enableFile: Boolean = true,
        logFilePrefix: String = "app", maxFileSize: Long = 10 * 1024 * 1024L, cacheDays: Int = 7
    ) {

        // 防止重复初始化
        if (initialized.get()) return
        initialized.set(true)

        this.enableConsole = isShow
        this.showStackInfo = showStackInfo
        this.enableFile = enableFile
        this.logFilePrefix = logFilePrefix
        this.maxFileSize = maxFileSize
        this.cacheDays = cacheDays

        // 创建日志目录
        logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        // 创建日志文件
        rotateLogFileIfNeeded()

        // 清理旧日志
        cleanOldLogs()

        // 崩溃捕获
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler()
        )

        d(TAG, "ALog init success")
    }

    /** 设置是否打印控制台日志 */
    fun setConsoleEnable(enable: Boolean) {
        enableConsole = enable
    }

    /** 设置是否写入文件 */
    fun setFileEnable(enable: Boolean) {
        enableFile = enable
    }

    /** 启用tag过滤 */
    fun enableTagFilter(enable: Boolean) {
        useTagFilter = enable
    }

    /** 添加允许打印的tag */
    fun addTag(tag: String) {
        enableTags.add(tag)
    }

    /** 清空tag */
    fun clearTags() {
        enableTags.clear()
    }

    // ================= 日志方法 =================

    fun d(tag: String, msg: String) = log(Log.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(Log.INFO, tag, msg)
    fun w(tag: String, msg: String) = log(Log.WARN, tag, msg)
    fun e(tag: String, msg: String) = log(Log.ERROR, tag, msg)
    fun f(tag: String, msg: String) = log(Log.ASSERT, tag, msg)

    /**
     * 核心日志方法
     */
    private fun log(priority: Int, tag: String, msg: String) {

        // tag过滤
        if (useTagFilter && !enableTags.contains(tag)) {
            return
        }

        val line = buildLogLine(msg)

        // 控制台输出
        if (enableConsole) {
            when (priority) {
                Log.DEBUG -> Log.d(tag, line)
                Log.INFO -> Log.i(tag, line)
                Log.WARN -> Log.w(tag, line)
                Log.ERROR -> Log.e(tag, line)
                Log.ASSERT -> Log.wtf(tag, line)
            }
        }

        // 文件输出
        if (enableFile) {
            writeAsync(line)
        }
    }

    /**
     * 构建日志行
     *
     * 格式：
     * [线程][pid-tid] (文件:行号)[方法] 内容
     */
    private fun buildLogLine(msg: String): String {

        val thread = Thread.currentThread().name
        val pid = Process.myPid()
        val tid = Process.myTid()

        if (!showStackInfo) {
            return "[$thread][$pid-$tid] $msg"
        }

        val (file, func, line) = getCallerInfo()

        return "[$thread][$pid-$tid] ($file:$line)[$func] $msg"
    }

    /**
     * 获取调用者信息
     */
    private fun getCallerInfo(): Triple<String, String, Int> {

        val stack = Thread.currentThread().stackTrace

        for (i in 5 until stack.size) {
            val e = stack[i]
            if (e.className.contains("ALog")) continue
            return Triple(
                e.fileName ?: "Unknown", e.methodName, e.lineNumber
            )
        }

        return Triple("Unknown", "Unknown", 0)
    }

    /**
     * 异步写入日志文件
     */
    private fun writeAsync(line: String) {

        executor.execute {
            try {
                rotateLogFileIfNeeded()
                fileWriter?.apply {
                    write("${dateFormat.format(Date())} $line\n")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 日志文件切割
     *
     * 按：
     * 1. 日期
     * 2. 文件大小
     *
     * 生成：
     * app-2026-03-26.log
     * app-2026-03-26-1.log
     */
    private fun rotateLogFileIfNeeded() {

        val date = fileDateFormat.format(Date())

        var index = 0
        var file: File

        do {

            val suffix = if (index == 0) "" else "-$index"
            val name = "$logFilePrefix-$date$suffix.log"

            file = File(logDir, name)

            index++

        } while (file.exists() && file.length() > maxFileSize)

        if (currentLogFile?.absolutePath == file.absolutePath) {
            return
        }

        try {
            fileWriter?.close()
        } catch (_: Exception) {
        }

        currentLogFile = file

        try {
            fileWriter = FileWriter(file, true)
        } catch (e: IOException) {
            fileWriter = null
        }
    }

    /**
     * 清理过期日志
     */
    private fun cleanOldLogs() {

        val files = logDir.listFiles() ?: return

        val expire = System.currentTimeMillis() - cacheDays * 24 * 60 * 60 * 1000L

        files.forEach {
            if (it.lastModified() < expire) {
                it.delete()
            }
        }
    }

    /**
     * 启动 logcat 捕获
     */
    fun startLogcatCapture() {

        if (!enableFile) return

        executor.execute {

            try {

                val file = File(
                    logDir, "logcat-${fileDateFormat.format(Date())}.log"
                )

                val process = Runtime.getRuntime().exec("logcat -v time")

                process.inputStream.bufferedReader().useLines { lines ->

                    FileWriter(file, true).use { writer ->
                        lines.forEach {
                            writer.write(it)
                            writer.write("\n")
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** 上传日志 */
    fun uploadLogs(upload: (File) -> Unit) {

        executor.execute {

            val files = logDir.listFiles() ?: return@execute

            files.forEach {
                upload(it)
            }
        }
    }

    /** flush */
    fun flush() {
        executor.execute {
            try {
                fileWriter?.flush()
            } catch (_: Exception) {
            }
        }
    }

    /** close */
    fun close() {
        executor.execute {
            try {
                fileWriter?.close()
            } catch (_: Exception) {
            }
        }
    }

    /** 获取日志目录 */
    fun getLogDir(): File = logDir

    /** 获取当前日志文件 */
    fun getCurrentLogFile(): File? = currentLogFile
}