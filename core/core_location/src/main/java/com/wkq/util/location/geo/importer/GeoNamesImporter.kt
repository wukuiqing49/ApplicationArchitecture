package com.wkq.util.location.geo.importer

import android.content.Context
import com.wkq.util.location.geo.repository.GeoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeoNamesImporter {

    /**
     * 在 App 首次冷启动或合适时机调用，提前在后台线程触发 Room 的 Asset 复制，
     * 避免在真正的照片反查过程中产生首个请求的复制等待。
     */
    suspend fun prepareDatabase(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            GeoRepository.init(context)
            // 触发一次反查，强制 Room 真正读取数据，从而触发 assets/geonames.db 物理复制
            GeoRepository.getGeoCoder(context).reverseGeocode(0.0, 0.0)
        }
    }
}
