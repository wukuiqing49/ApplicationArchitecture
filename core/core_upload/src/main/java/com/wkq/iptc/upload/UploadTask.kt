package com.wkq.iptc.upload

data class UploadTask(
    val recordId: Long,
    val recordUri: String,
    val fileName: String,
    /** 鏂偣缁紶璧峰鍋忕Щ锛堝瓧鑺傦級銆? 琛ㄧず浠庡ご浼犮€傜敱 UploadWorker 浠庢暟鎹簱涓鍙栧悗濉叆銆?*/
    val resumeOffset: Long = 0L
)

