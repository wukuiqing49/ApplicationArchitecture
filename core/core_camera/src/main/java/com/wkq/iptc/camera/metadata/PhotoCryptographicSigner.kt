package com.wkq.iptc.camera.metadata

import android.location.Location
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date

object PhotoCryptographicSigner {

    data class SignatureData(
        val hash: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val creator: String
    )

    fun signPhoto(file: File, location: Location?, timestamp: Long, creator: String? = null): Result<Unit> {
        return runCatching {
            // 1. 先把基础 GPS 和日期信息写入并保存，确保不包含签名注释
            val exif = ExifInterface(file)
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
            val now = formatter.format(Date(timestamp))
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, now)
            if (location != null) {
                exif.setGpsInfo(location)
            }
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, null)
            exif.saveAttributes()

            // 2. 仅读取 JPEG 图片的像素数据部分（过滤 APP 段与元数据），流式计算 SHA-256 图像指纹
            val pixelHash = calculatePixelHash(file)

            // 3. 构造包含图像指纹、坐标、拍摄时间、原作者的签名元数据
            val summary = buildString {
                append("PRESSIPTC_SIGN:")
                append(pixelHash)
                append("|")
                append(location?.latitude ?: 0.0)
                append(",")
                append(location?.longitude ?: 0.0)
                append("|")
                append(timestamp)
                if (!creator.isNullOrBlank()) {
                    append("|")
                    append(creator)
                }
            }

            // 4. 将签名元数据写入 USER_COMMENT 并保存
            val signedExif = ExifInterface(file)
            signedExif.setAttribute(ExifInterface.TAG_USER_COMMENT, summary)
            signedExif.saveAttributes()
        }
    }

    fun parseSignature(file: File): SignatureData? {
        return runCatching {
            val exif = ExifInterface(file)
            val comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty()
            if (!comment.startsWith("PRESSIPTC_SIGN:")) return null
            val parts = comment.removePrefix("PRESSIPTC_SIGN:").split("|")
            val hash = parts.getOrNull(0) ?: return null
            val gpsPart = parts.getOrNull(1)
            val coords = gpsPart?.split(",")
            val lat = coords?.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val lng = coords?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val time = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            val creator = parts.getOrNull(3).orEmpty()
            SignatureData(hash, lat, lng, time, creator)
        }.getOrNull()
    }

    fun verifyPhotoSignature(file: File): Result<Unit> {
        return runCatching {
            val exif = ExifInterface(file)
            val comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty()
            require(comment.startsWith("PRESSIPTC_SIGN:")) { "Missing signature comment" }

            // 解析 comment 中的数据
            val parts = comment.removePrefix("PRESSIPTC_SIGN:").split("|")
            val expectedHash = parts.getOrNull(0) ?: throw IllegalArgumentException("Invalid signature format")
            val gpsPart = parts.getOrNull(1)
            val timePart = parts.getOrNull(2)?.toLongOrNull()

            // 1. 验证核心图像像素哈希，无需读写盘或清除签名，绝不破坏原有 IPTC/EXIF/XMP 块
            val currentHash = calculatePixelHash(file)

            require(expectedHash.equals(currentHash, ignoreCase = true)) {
                "Photo image pixels have been tampered with"
            }

            // 2. 验证 GPS 坐标防篡改
            if (!gpsPart.isNullOrBlank()) {
                val coords = gpsPart.split(",")
                val expectedLat = coords.getOrNull(0)?.toDoubleOrNull()
                val expectedLng = coords.getOrNull(1)?.toDoubleOrNull()
                val currentLatLng = exif.latLong
                if (expectedLat != null && expectedLng != null && expectedLat != 0.0 && expectedLng != 0.0) {
                    val currentLat = currentLatLng?.get(0) ?: 0.0
                    val currentLng = currentLatLng?.get(1) ?: 0.0
                    val latDiff = Math.abs(expectedLat - currentLat)
                    val lngDiff = Math.abs(expectedLng - currentLng)
                    // 允许极微小的数值精度差异范围，超出视为修改
                    require(latDiff < 1e-5 && lngDiff < 1e-5) {
                        "GPS coordinates have been tampered with"
                    }
                }
            }

            // 3. 验证拍摄时间防篡改
            if (timePart != null && timePart > 0L) {
                val currentTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                if (!currentTimeStr.isNullOrBlank()) {
                    val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                    val expectedTimeStr = formatter.format(Date(timePart))
                    require(expectedTimeStr == currentTimeStr) {
                        "Capture time has been tampered with"
                    }
                }
            }
        }
    }

    /**
     * 流式计算图片像素数据的 SHA-256 哈希，避免将整图加载至内存中
     */
    private fun calculatePixelHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val offset = getJpegPixelOffset(file)

        file.inputStream().buffered().use { input ->
            if (offset > 0) {
                var skipped = 0L
                while (skipped < offset) {
                    val curSkipped = input.skip(offset - skipped)
                    if (curSkipped <= 0) {
                        if (input.read() == -1) break
                        skipped++
                    } else {
                        skipped += curSkipped
                    }
                }
            }
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 流式扫描 JPEG 文件，跳过所有的 APP 标记段（如 APP0、APP1-EXIF、APP13-IPTC 等），
     * 定位至 SOS (Start of Scan, 0xFFDA) 段的起始位置偏移量。
     * 采用 O(1) 内存的流式跳过逻辑，彻底规避 OOM。
     */
    private fun getJpegPixelOffset(file: File): Long {
        return runCatching {
            file.inputStream().buffered().use { input ->
                val header = ByteArray(2)
                if (input.read(header) != 2) return 0L
                if ((header[0].toInt() and 0xFF) != 0xFF || (header[1].toInt() and 0xFF) != 0xD8) {
                    return 0L
                }

                var offset = 2L
                while (true) {
                    val b = input.read()
                    if (b == -1) break
                    offset++

                    if (b == 0xFF) {
                        // 跳过连续的 0xFF 填充字节
                        var nextB = input.read()
                        if (nextB == -1) break
                        offset++
                        while (nextB == 0xFF) {
                            nextB = input.read()
                            if (nextB == -1) break
                            offset++
                        }

                        val marker = nextB
                        if (marker == 0x00) {
                            continue
                        }

                        // SOS (Start of Scan) - 图像实际像素流数据的起点
                        if (marker == 0xDA) {
                            // 返回 0xFF 0xDA 的起始位置，即当前偏移量减去 2 字节（0xFF 和 0xDA）
                            return offset - 2
                        }

                        // EOI (End of Image)
                        if (marker == 0xD9) {
                            break
                        }

                        // rst0~rst7 与 tem 无长度参数，其他段有 2 字节的长度信息
                        val hasLength = !(marker in 0xD0..0xD7 || marker == 0x01)
                        if (hasLength) {
                            val lenH = input.read()
                            val lenL = input.read()
                            if (lenH == -1 || lenL == -1) break
                            offset += 2
                            val segLength = ((lenH and 0xFF) shl 8) or (lenL and 0xFF)
                            val skipBytes = segLength - 2L
                            if (skipBytes > 0) {
                                var skipped = 0L
                                while (skipped < skipBytes) {
                                    val curSkipped = input.skip(skipBytes - skipped)
                                    if (curSkipped <= 0) {
                                        if (input.read() == -1) break
                                        skipped++
                                        offset++
                                    } else {
                                        skipped += curSkipped
                                        offset += curSkipped
                                    }
                                }
                            }
                        }
                    }
                }
                0L
            }
        }.getOrDefault(0L)
    }
}
