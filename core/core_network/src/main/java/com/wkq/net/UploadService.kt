package com.wkq.net

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 文件上传示例接口
 */
interface UploadService {

    /**
     * 单文件上传
     * @param file 文件部分 (使用 [com.wkq.net.core.toMultipartPart] 或 [com.wkq.net.core.toProgressPart] 创建)
     * @param description 附加描述信息
     */
    @Multipart
    @POST("api/upload/file")
    fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Call<BaseResponse<String>>

    /**
     * 多文件上传
     * @param files 多个文件部分
     */
    @Multipart
    @POST("api/upload/files")
    fun uploadFiles(
        @Part files: List<MultipartBody.Part>
    ): Call<BaseResponse<List<String>>>
}
