package com.wkq.net

import com.wkq.net.config.NetConfig
import com.wkq.net.core.ApiRetrofit
import com.wkq.net.core.DownloadRetrofit
import com.wkq.net.core.NetManager
import com.wkq.net.core.downloadFileFlow
import com.wkq.net.core.ApiResponse
import com.wkq.net.core.DownloadState
import com.wkq.net.core.awaitRawResult
import com.wkq.net.core.onSuccess
import com.wkq.net.core.onError
import com.wkq.net.core.awaitResult
import com.wkq.net.interceptor.HeaderInterceptor
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
            .setBaseUrl("https://api.example.com/") // 主的
            .putBaseUrl("google", "https://google.com/") // 演示动态域名
            .putBaseUrl("github", "https://api.github.com/") // 演示动态域名
            .setConnectTimeout(15L)
            .setReadTimeout(20L)
            .setWriteTimeout(20L)
            .setDebugLogsEnabled(true)
            .addDefaultHeader("Global-Version", "1.0.0")
            .build()
            
        NetManager.init(config)
    }

    /**
     * 示例接口定义，演示如何使用 @Headers 切换 BaseUrl
     */
    interface ThirdPartyService {
        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}:google")
        @GET("search")
        fun searchGoogle(@Query("q") query: String): Call<ResponseBody>

        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}:github")
        @GET("users/{user}/repos")
        fun getGithubRepos(@Path("user") user: String): Call<ResponseBody>

        // 5. 演示返回非 BaseResponse 格式 (例如直接返回 Any 或自定义 Bean)
        @Headers("${HeaderInterceptor.HEADER_BASE_URL_KEY}:google")
        @GET("search")
        fun searchGoogleAny(@Query("q") query: String): Call<Any>
    }

    // 初始化后，可以懒加载代理
    private val apiService by lazy { ApiRetrofit.create(ApiService::class.java) }
    private val downloadService by lazy { DownloadRetrofit.create(DownloadService::class.java) }
    private val thirdPartyService by lazy { ApiRetrofit.create(ThirdPartyService::class.java) }

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

    /**
     * 7. 演示请求非标准格式的接口 (不使用 BaseResponse 包装)
     * 场景：请求第三方 API (如 Google/GitHub)，其返回结构不符合我们的 BaseResponse。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testNonStandardApi() {
        GlobalScope.launch(Dispatchers.Main) {
            println("▶ 开始请求非标准接口 (返回 Any)...")
            
            // 使用 awaitRawResult() 代替 awaitResult()，它不会校验 body.code
            thirdPartyService.searchGoogleAny("Kotlin").awaitRawResult()
                .onSuccess { data ->
                    // 这里的 data 类型是 Any (对于 JSON 来说通常是 Map 或 List)
                    println("★ 非标准接口请求成功！返回数据: $data")
                }
                .onError { code, message ->
                    println("★ 非标准接口请求失败: [$code] $message")
                }
        }
    }
}
