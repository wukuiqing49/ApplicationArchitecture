package com.wkq.iptc.upload.http

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface HttpUploadService {
    @Multipart
    @POST
    fun uploadFile(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @GET
    fun testConnection(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Call<ResponseBody>
}

