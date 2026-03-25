package com.wkq.net

import com.wkq.net.core.toMultipartPart
import com.wkq.net.core.toProgressPart
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * 文件上传功能演示单元测试
 */
class FileUploadDemoTest {

    @Test
    fun testFileToMultipartPart() {
        // 1. 创建临时测试文件
        val testFile = File.createTempFile("test_upload", ".txt")
        testFile.deleteOnExit()
        FileOutputStream(testFile).use { fos ->
            fos.write("Hello, this is a test file for upload demo.".toByteArray())
        }

        // 2. 演示转换为标准的 MultipartBody.Part
        val part = testFile.toMultipartPart(fieldName = "my_file", fileName = "hello.txt")
        
        // 3. 验证
        assertNotNull(part)
        // 注意：okhttp 的 headers 并不直接暴露这些内部字段的原始值，但我们可以通过 headers() 检查
        val contentDisposition = part.headers?.get("Content-Disposition")
        assertNotNull(contentDisposition)
        assert(contentDisposition!!.contains("name=\"my_file\""))
        assert(contentDisposition!!.contains("filename=\"hello.txt\""))
        
        println("成功创建标准 MultipartBody.Part")
    }

    @Test
    fun testFileToProgressPart() {
        // 1. 创建临时测试文件
        val testFile = File.createTempFile("test_progress", ".txt")
        testFile.deleteOnExit()
        val content = "A".repeat(1024 * 10) // 10KB
        testFile.writeText(content)

        var lastProgress = -1
        
        // 2. 演示转换为带进度的 MultipartBody.Part
        val part = testFile.toProgressPart(fieldName = "progress_file") { percent ->
            println("上传进度: $percent%")
            lastProgress = percent
        }

        // 3. 模拟 OkHttp 写入数据以触发进度回调
        val okioSink = okio.Buffer()
        part.body.writeTo(okioSink)
        
        // 4. 验证进度最终应为 100%
        assertEquals(100, lastProgress)
    }

    /**
     * 这里演示如何在 ApiService 中使用
     */
    fun demoServiceUsage() {
        /*
        val file = File("/path/to/your/image.jpg")
        
        // 创建带进度的 Part
        val filePart = file.toProgressPart("avatar") { percent ->
            // UI 更新
        }
        
        // 其他字段
        val descPart = "My Avatar".toRequestBody()
        
        // 调用接口 (协程)
        GlobalScope.launch {
            val apiService = ApiRetrofit.create(UploadService::class.java)
            apiService.uploadFile(filePart, descPart).awaitResult()
                .onSuccess { data -> println("上传成功: $data") }
                .onError { code, msg -> println("上传失败: $msg") }
        }
        */
    }
}
