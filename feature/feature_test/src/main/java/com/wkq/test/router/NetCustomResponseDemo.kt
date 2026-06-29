package com.wkq.test.router

import com.wkq.net.config.NetConfig
import com.wkq.net.core.ApiResponse
import com.wkq.net.core.Net
import com.wkq.net.core.NetResponseParser
import com.wkq.net.core.NetResponseParserFactory
import retrofit2.http.GET

/**
 * 演示非 code/message/data 响应壳。
 */
data class DemoApiResult<T>(
    val status: Int,
    val msg: String?,
    val result: T?
)

class DemoApiResultParser<T> : NetResponseParser<DemoApiResult<T>, T> {
    override fun isSuccess(response: DemoApiResult<T>): Boolean {
        return response.status == 0
    }

    override fun code(response: DemoApiResult<T>): Int {
        return response.status
    }

    override fun message(response: DemoApiResult<T>): String? {
        return response.msg
    }

    override fun data(response: DemoApiResult<T>): T? {
        return response.result
    }
}

class DemoApiResultParserFactory : NetResponseParserFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <R, T> create(): NetResponseParser<R, T> {
        return DemoApiResultParser<T>() as NetResponseParser<R, T>
    }
}

data class DemoUser(
    val id: Long,
    val name: String
)

data class GithubRepo(
    val id: Long,
    val name: String,
    val full_name: String?
)

interface DemoNetApi {
    @GET("user/info")
    suspend fun getUserInfo(): DemoApiResult<DemoUser>

    @GET("users/octocat/repos")
    suspend fun getGithubRepos(): List<GithubRepo>
}

/**
 * 示例：项目主后台使用全局响应壳，第三方接口直接返回原始结构。
 */
object NetCustomResponseDemo {

    fun createConfig(): NetConfig {
        return NetConfig.Builder()
            .setBaseUrl("https://api.xxx.com/")
            .setDefaultResponseParserFactory(DemoApiResultParserFactory())
            .build()
    }

    suspend fun requestUser(api: DemoNetApi): ApiResponse<DemoUser> {
        return Net.requestWithDefaultParser {
            api.getUserInfo()
        }
    }

    suspend fun requestGithubRepos(api: DemoNetApi): ApiResponse<List<GithubRepo>> {
        return Net.raw {
            api.getGithubRepos()
        }
    }
}
