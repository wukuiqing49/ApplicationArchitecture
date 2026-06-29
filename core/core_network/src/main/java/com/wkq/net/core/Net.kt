package com.wkq.net.core

import com.wkq.net.BaseResponse
import java.io.File
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import retrofit2.Call

/**
 * 网络请求统一入口，业务层优先使用这里的便捷方法。
 */
object Net {

    fun <T> create(serviceClass: Class<T>): T {
        return ApiRetrofit.create(serviceClass)
    }

    suspend fun <T> request(apiCall: suspend () -> BaseResponse<T>): ApiResponse<T> {
        return safeApiCall(apiCall)
    }

    suspend fun <R, T> request(
        apiCall: suspend () -> R,
        parser: NetResponseParser<R, T>
    ): ApiResponse<T> {
        return safeApiCall(apiCall, parser)
    }

    suspend fun <R, T> requestWithDefaultParser(apiCall: suspend () -> R): ApiResponse<T> {
        return safeApiCallWithDefaultParser(apiCall)
    }

    suspend fun <T> raw(apiCall: suspend () -> T): ApiResponse<T> {
        return safeRawApiCall(apiCall)
    }

    suspend fun <T> call(call: Call<BaseResponse<T>>): ApiResponse<T> {
        return call.awaitResult()
    }

    suspend fun <R, T> call(
        call: Call<R>,
        parser: NetResponseParser<R, T>
    ): ApiResponse<T> {
        return call.awaitResult(parser)
    }

    suspend fun <R, T> callWithDefaultParser(call: Call<R>): ApiResponse<T> {
        return call.awaitResultWithDefaultParser()
    }

    suspend fun <T> rawCall(call: Call<T>): ApiResponse<T> {
        return call.awaitRawResult()
    }

    fun <T> upload(
        file: File,
        fieldName: String = "file",
        fileName: String? = null,
        apiCall: suspend (MultipartBody.Part) -> BaseResponse<T>
    ): Flow<UploadState<T>> {
        return uploadFlow(file, fieldName, fileName, apiCall)
    }

    fun <R, T> upload(
        file: File,
        fieldName: String = "file",
        fileName: String? = null,
        parser: NetResponseParser<R, T>,
        apiCall: suspend (MultipartBody.Part) -> R
    ): Flow<UploadState<T>> {
        return uploadFlow(file, fieldName, fileName, parser, apiCall)
    }

    fun <R, T> uploadWithDefaultParser(
        file: File,
        fieldName: String = "file",
        fileName: String? = null,
        apiCall: suspend (MultipartBody.Part) -> R
    ): Flow<UploadState<T>> {
        return uploadFlowWithDefaultParser(file, fieldName, fileName, apiCall)
    }

    fun <T> uploadRaw(
        file: File,
        fieldName: String = "file",
        fileName: String? = null,
        apiCall: suspend (MultipartBody.Part) -> T
    ): Flow<UploadState<T>> {
        return uploadRawFlow(file, fieldName, fileName, apiCall)
    }

    fun <T> uploadFiles(
        files: List<File>,
        fieldName: String = "files",
        apiCall: suspend (List<MultipartBody.Part>) -> BaseResponse<T>
    ): Flow<UploadState<T>> {
        return uploadFilesFlow(files, fieldName, apiCall)
    }

    fun <R, T> uploadFiles(
        files: List<File>,
        fieldName: String = "files",
        parser: NetResponseParser<R, T>,
        apiCall: suspend (List<MultipartBody.Part>) -> R
    ): Flow<UploadState<T>> {
        return uploadFilesFlow(files, fieldName, parser, apiCall)
    }

    fun <R, T> uploadFilesWithDefaultParser(
        files: List<File>,
        fieldName: String = "files",
        apiCall: suspend (List<MultipartBody.Part>) -> R
    ): Flow<UploadState<T>> {
        return uploadFilesFlowWithDefaultParser(files, fieldName, apiCall)
    }

    fun <T> uploadRawFiles(
        files: List<File>,
        fieldName: String = "files",
        apiCall: suspend (List<MultipartBody.Part>) -> T
    ): Flow<UploadState<T>> {
        return uploadRawFilesFlow(files, fieldName, apiCall)
    }
}
