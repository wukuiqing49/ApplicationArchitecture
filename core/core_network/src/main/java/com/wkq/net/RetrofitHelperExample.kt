package com.wkq.net

import com.wkq.net.config.NetConfig
import com.wkq.net.core.ApiRetrofit
import com.wkq.net.core.DownloadRetrofit
import com.wkq.net.core.NetManager
import com.wkq.net.core.downloadFileFlow
import com.wkq.net.core.ApiResponse
import com.wkq.net.core.DownloadState
import com.wkq.net.core.onSuccess
import com.wkq.net.core.onError
import com.wkq.net.core.awaitResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.io.File

/**
 * 示例数据类
 */
data class CloudFileInfo(
    val id: String,
    val name: String,
    val size: Long
)

/**
 * 示例接口定义 (JSON API)
 */
interface ApiService {

    // 1. GET 请求示例（支持 query 参数）
    @GET("api/music/list")
    fun getMusicList(@Query("page") page: Int): Call<BaseResponse<List<CloudFileInfo>>>

    // 2. POST/Form 表单提交示例（支持字段参数）
    @FormUrlEncoded
    @POST("api/music/upload")
    fun uploadMusic(
        @Field("title") title: String,
        @Field("artist") artist: String
    ): Call<BaseResponse<CloudFileInfo>>
}

/**
 * 下载专用接口定义
 */
interface DownloadService {
    // 3. 文件下载示例（带 @Streaming 注解，防止大文件 OOM）
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody
}

/**
 * 高级网络框架示例调用
 */
class AdvancedNetworkExample {

    /**
     * 0. 框架初始化 (通常在 Application.onCreate 中调用)
     */
    fun initFramework() {
        val config = NetConfig.Builder()
            .setBaseUrl("https://api.example.com/")
            .setConnectTimeout(15L) // 15秒连接超时
            .setReadTimeout(20L)    // 20秒读取超时
            .setWriteTimeout(20L)   // 20秒写入超时
            .setDebugLogsEnabled(true) // 开启详细日志，拦截器自动格式化
            .addDefaultHeader("Global-Version", "1.0.0") // 配置默认 Header
            // 注册全局业务响应处理器 (核心演示)
            .setGlobalHandler(object : com.wkq.net.core.GlobalNetHandler {
                override fun onHandleBusinessCode(code: Int, message: String?): Boolean {
                    return when (code) {
                        401 -> {
                            // 示例：处理登录过期
                            println("★ [全局拦截] 检测到 Token 过期 (401)，正在跳转登录页...")
                            // 此处通常执行：ARouter.getInstance().build("/app/login").navigation()
                            true // 返回 true 表示该 code 已由全局处理，业务层不再需要弹窗或逻辑处理
                        }
                        500 -> {
                            // 示例：处理服务器内部错误
                            println("★ [全局拦截] 服务器开小差了 (500)，正在上报错误日志...")
                            false // 返回 false 表示仍希望具体的业务调用方收到 Error 回调并进一步处理
                        }
                        else -> false
                    }
                }
            })
            .build()
            
        NetManager.init(config)
    }

    // 初始化后，可以懒加载 ApiRetrofit 和 DownloadRetrofit 代理
    private val apiService by lazy { ApiRetrofit.create(ApiService::class.java) }
    private val downloadService by lazy { DownloadRetrofit.create(DownloadService::class.java) }

    // 用于保存当前的协程 Job 以便取消
    private var currentJob: Job? = null

    /**
     * 1. GET 请求示例展示 (协程方式)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testGetMusicList() {
        // 动态添加 Header
        NetManager.headerInterceptor.addHeader("Authorization", "Bearer token_for_get")

        // 实际开发中通常使用 lifecycleScope.launch 或 viewModelScope.launch
        currentJob = GlobalScope.launch(Dispatchers.Main) {
            // 直接在一行调用 .awaitResult()，享受协程和 ApiResponse 链式语法的优势！
            apiService.getMusicList(1).awaitResult()
                .onSuccess { data ->
                    println("获取音乐列表成功: $data")
                }
                .onError { code, message ->
                    println("获取音乐列表失败 [错误码 $code]: $message")
                }
        }
    }

    /**
     * 2. POST/Form 请求示例展示 (协程方式)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testUploadMusic() {
        NetManager.headerInterceptor.addHeader("Authorization", "Bearer token_for_upload")

        GlobalScope.launch(Dispatchers.Main) {
            // 使用协程扩展方法 awaitResult()
            apiService.uploadMusic("My Song", "My Artist").awaitResult()
                .onSuccess { data ->
                    println("上传音乐成功: $data")
                }
                .onError { code, message ->
                    println("上传音乐失败 [错误码 $code]: $message")
                }
        }
    }

    /**
     * 3. 文件下载示例展示 (利用 Flow 获取实时进度)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testDownloadFile() {
        // 目标保存路径
        val destFile = File("/storage/emulated/0/Download/music.mp3")

        GlobalScope.launch(Dispatchers.Main) {
            try {
                // 第一步：先通过协程获取 ResponseBody
                val responseBody = downloadService.downloadFile("https://example.com/music.mp3")
                
                // 第二步：使用扩展方法并收集下载进度的 Flow
                responseBody.downloadFileFlow(destFile).collect { state ->
                    when (state) {
                        is DownloadState.Progress -> {
                            println("正在下载... ${state.percent}% (${state.currentLength}/${state.totalLength})")
                        }
                        is DownloadState.Success -> {
                            println("下载成功！保存至: ${state.file.absolutePath}")
                        }
                        is DownloadState.Error -> {
                            println("下载失败 [${state.code}]: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // 捕获网络连接或 IO 异常
                println("请求失败: ${e.message}")
            }
        }
    }

    /**
     * 4. 取消请求示例
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun cancelCurrentRequest() {
        currentJob?.let {
            if (it.isActive) {
                it.cancel() // 一键取消协程内的所有网络活动
                println("当前任务已被手动取消。")
            }
        }
    }

    // ==========================================
    // 高阶用法：串行与并发控制示例
    // ==========================================

    /**
     * 5. 串联（依赖型）请求示例：
     * 场景：先获取列表拿 ID，再根据 ID 更新数据。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testSequentialRequests() {
        GlobalScope.launch(Dispatchers.Main) {
            println("▶ 开始串行请求...")
            
            // 步骤 A
            val listResponse = apiService.getMusicList(1).awaitResult()
            
            if (listResponse is ApiResponse.Success) {
                val firstMusic = listResponse.data?.firstOrNull()
                
                if (firstMusic != null) {
                    println("请求 A 成功，结果作为 B 的输入：${firstMusic.name}")
                    
                    // 步骤 B
                    val uploadResponse = apiService.uploadMusic(firstMusic.name, "New Artist").awaitResult()
                    uploadResponse.onSuccess { 
                        println("★ 串联最终完成！数据: $it") 
                    }.onError { code, msg -> 
                        println("更新失败: [$code] $msg") 
                    }
                }
            } else {
                val error = listResponse as ApiResponse.Error
                println("请求 A 失败，中止流程: [${error.code}] ${error.message}")
            }
        }
    }

    /**
     * 6. 并发（等待全完成）请求示例：
     * 场景：同时拉取三个独立板块的数据。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testConcurrentRequests() {
        GlobalScope.launch(Dispatchers.Main) {
            println("▶ 并发起跑 3 个网络请求...")
            
            val deferredTask1 = async { apiService.getMusicList(1).awaitResult() }
            val deferredTask2 = async { apiService.getMusicList(2).awaitResult() }
            val deferredTask3 = async { apiService.getMusicList(3).awaitResult() }
            
            // 齐头并进，总耗时为最慢的那个
            val result1 = deferredTask1.await()
            val result2 = deferredTask2.await()
            val result3 = deferredTask3.await()
            
            println("★ 所有并发请求都已回归！数据已准备好合并渲染。")
            
            if (result1 is ApiResponse.Success && result2 is ApiResponse.Success && result3 is ApiResponse.Success) {
                println("太棒了，3个接口全通！总数据量: ${result1.data?.size} + ${result2.data?.size} + ${result3.data?.size}")
            } else {
                println("部分接口失败。")
            }
        }
    }
}
