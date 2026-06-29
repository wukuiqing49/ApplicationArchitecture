package com.wkq.net.core

import com.wkq.net.BaseResponse

/**
 * 业务响应壳解析器。
 *
 * 网络库不固定 code/message/data 字段名，业务可按自己的返回结构实现解析器。
 */
interface NetResponseParser<R, T> {
    fun isSuccess(response: R): Boolean
    fun code(response: R): Int
    fun message(response: R): String?
    fun data(response: R): T?
}

/**
 * 默认响应壳解析器工厂。
 *
 * 适合在 NetConfig 中配置项目主后台的统一响应结构，避免每个请求都手动传 parser。
 */
interface NetResponseParserFactory {
    fun <R, T> create(): NetResponseParser<R, T>
}

/**
 * 默认响应壳解析器，兼容 code/message/data 结构。
 */
class BaseResponseParser<T> : NetResponseParser<BaseResponse<T>, T> {
    override fun isSuccess(response: BaseResponse<T>): Boolean {
        return response.isSuccess()
    }

    override fun code(response: BaseResponse<T>): Int {
        return response.code
    }

    override fun message(response: BaseResponse<T>): String? {
        return response.message
    }

    override fun data(response: BaseResponse<T>): T? {
        return response.data
    }
}

/**
 * 默认 code/message/data 响应壳解析器工厂。
 */
object BaseResponseParserFactory : NetResponseParserFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <R, T> create(): NetResponseParser<R, T> {
        return BaseResponseParser<T>() as NetResponseParser<R, T>
    }
}
