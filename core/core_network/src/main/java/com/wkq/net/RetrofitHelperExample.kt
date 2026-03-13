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

    // 1. GET 请求示例（支持 query 参数） - 恢复原有的 Call 定义
    @GET("api/music/list")
    fun getMusicList(@Query("page") page: Int): Call<BaseResponse<List<CloudFileInfo>>>

    // 2. POST/Form 表单提交示例（支持字段参数）- 恢复原有的 Call 定义
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
                    println("Get Music List Success: $data")
                }
                .onError { code, message ->
                    println("Get Music List Failed with code $code: $message")
                }
        }
    }

    /**
     * 2. POST/Form 请求示例展示 (并发执行示例)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testUploadMusic() {
        NetManager.headerInterceptor.addHeader("Authorization", "Bearer token_for_upload")

        GlobalScope.launch(Dispatchers.Main) {
            // 和以前的 Callback 格式几乎一模一样，但这是原生的 Coroutines!
            apiService.uploadMusic("My Song", "My Artist").awaitResult()
                .onSuccess { data ->
                    println("Upload Music Success: $data")
                }
                .onError { code, message ->
                    println("Upload Music Failed with code $code: $message")
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
                // 第一步：先通过协程获取 ResponseBody (由于标了 @Streaming，这一步并不会立刻读取完成)
                val responseBody = downloadService.downloadFile("https://example.com/music.mp3")
                
                // 第二步：使用扩展方法并收集下载进度的 Flow
                responseBody.downloadFileFlow(destFile).collect { state ->
                    when (state) {
                        is DownloadState.Progress -> {
                            println("Downloading... ${state.percent}% (${state.currentLength}/${state.totalLength})")
                        }
                        is DownloadState.Success -> {
                            println("Download Success! Saved to: ${state.file.absolutePath}")
                        }
                        is DownloadState.Error -> {
                            println("Download Failed [${state.code}]: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // 捕获获取流之前的连接等异常
                println("Request Failed: ${e.message}")
            }
        }
    }

    /**
     * 4. 取消请求示例 (协程的魅力)
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun cancelCurrentRequest() {
        currentJob?.let {
            if (it.isActive) {
                it.cancel() // 一键取消协程内的所有网络活动
                println("Job has been manually canceled.")
            }
        }
    }

    // ==========================================
    // 高阶进阶用法：串行与并发控制示例
    // ==========================================

    /**
     * 5. 串联（依赖型）请求示例：
     * 核心：利用协程的同步特性，上一步的输出作为下一步的输入。
     * 场景：先获取音乐列表的第一首音乐，拿到 ID 后再去请求并更新它的播放量。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testSequentialRequests() {
        GlobalScope.launch(Dispatchers.Main) {
            println("▶ 开始串行请求...")
            
            // 步骤 A：先请求第一个接口 
            val listResponse = apiService.getMusicList(1).awaitResult()
            
            // 如果 A 请求成功，我们再拿着它的结果进行请求 B
            if (listResponse is ApiResponse.Success) {
                val firstMusic = listResponse.data?.firstOrNull()
                
                if (firstMusic != null) {
                    println("请求 A 成功！拿到第一首歌: ${firstMusic.name} (ID: ${firstMusic.id})")
                    
                    // 步骤 B：拿着第一首歌的信息发起上传/更新请求
                    val uploadResponse = apiService.uploadMusic(firstMusic.name, "New Artist").awaitResult()
                    
                    uploadResponse.onSuccess { 
                        println("★ 串联最终完成！更新后的音乐数据: $it") 
                    }.onError { code, msg -> 
                        println("更新接口失败: [$code] $msg") 
                    }
                } else {
                    println("音乐列表为空，无法进行第二步")
                }
            } else {
                // 如果 A 请求直接挂了，压根就不会执行到 B 请求
                val error = listResponse as ApiResponse.Error
                println("请求 A 直接失败，中止后续流程: [${error.code}] ${error.message}")
            }
        }
    }

    /**
     * 6. 并发（等待全完成）请求示例：
     * 核心：利用 async 和 await()，让耗时不同的接口齐头并进，然后统一收口。
     * 场景：用户打开个人首页，同时去拉取“我的详情”、“VIP状态”、“历史听歌记录”这三个毫不相干的接口。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun testConcurrentRequests() {
        GlobalScope.launch(Dispatchers.Main) {
            println("▶ 准备并发起跑 3 个网络请求...")
            
            // 仅仅是展示并发技巧，复用同一个接口进行演示
            // async 表示“发起一个后台异步任务，我会在未来去拿你的结果 (Deferred<ApiResponse<T>>)”
            val deferredTask1 = async { apiService.getMusicList(1).awaitResult() }
            val deferredTask2 = async { apiService.getMusicList(2).awaitResult() }
            val deferredTask3 = async { apiService.getMusicList(3).awaitResult() }
            
            println("3 个请求已经同时在路上了！不要阻塞我干别的活...")
            
            // await() 才是真正的【等待收割并阻塞当前协程逻辑】的点
            // 这一步的总耗时 = 这三个请求里耗时最长的那一个！极大地优化了体验。
            val result1 = deferredTask1.await()
            val result2 = deferredTask2.await()
            val result3 = deferredTask3.await()
            
            // 等全部到位之后，我们就可以安全地合并数据来刷新 UI 啦！
            println("★ 所有并发请求都已回归！现在来判断是否全部成功：")
            
            val isAllSuccess = result1 is ApiResponse.Success && 
                               result2 is ApiResponse.Success && 
                               result3 is ApiResponse.Success
                               
            if (isAllSuccess) {
                // 这个时候你就能拿到所有的数据了
                val data1 = result1.data
                val data2 = result2.data
                val data3 = result3.data
                
                println("太棒了，3个接口全通！即将把海量数据合并渲染进入主 UI！总数据量: ${data1?.size} + ${data2?.size} + ${data3?.size}")
            } else {
                println("很抱歉，3个并发接口里有 1个 或多个失败了！页面可能需要展示部分异常状态。")
                // 你可以去分别检查 result1 / result2 / result3 谁是 Error 类型
            }
        }
    }
}
