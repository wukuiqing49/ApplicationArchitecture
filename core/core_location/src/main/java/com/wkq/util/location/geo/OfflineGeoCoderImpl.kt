package com.wkq.util.location.geo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.wkq.util.location.geo.dao.GeoDao
import com.wkq.util.location.geo.entity.CityEntity
import com.wkq.util.location.geo.util.GeoDistanceUtils
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale

class OfflineGeoCoderImpl(
    private val geoDao: GeoDao,
    private val context: Context? = null
) : OfflineGeoCoder {

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): OfflineLocation? =
        withContext(Dispatchers.IO) {
            // 1. 经纬度合法性校验
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                return@withContext null
            }

            // 2. 第一次查询：±0.5度范围 (约 55 公里)
            var nearbyCities = queryNearby(latitude, longitude, 0.5)

            // 3. 降级重试：若找不到，扩大至 ±1.5度 (约 165 公里)
            if (nearbyCities.isEmpty()) {
                nearbyCities = queryNearby(latitude, longitude, 1.5)
            }

            if (nearbyCities.isEmpty()) {
                // 如果本地离线数据库无该定位的任何地名数据，且 Context 不为空，采用系统原生 Geocoder 进行在线反编码
                val onlineLoc = performOnlineGeocode(latitude, longitude)
                if (onlineLoc != null) {
                    return@withContext onlineLoc
                }
                return@withContext null
            }

            // 4. 内存中计算 Haversine 距离，取最近城市
            var closestCity: CityEntity? = null
            var minDistance = Double.MAX_VALUE

            for (city in nearbyCities) {
                val distance = GeoDistanceUtils.haversine(
                    latitude, longitude,
                    city.latitude, city.longitude
                )
                if (distance < minDistance) {
                    minDistance = distance
                    closestCity = city
                }
            }

            val city = closestCity ?: return@withContext null

            // 5. 联查国家与省州
            val country = geoDao.getCountry(city.countryCode)
            val admin1 = geoDao.getAdmin1(city.countryCode, city.admin1Code)

            // 6. 系统语言环境国际化判断 (全面支持多国语言自适应)
            val systemLang = Locale.getDefault().language
            val isZh = systemLang == "zh"

            // 国家名字：利用 Android 系统的 Locale 进行 100% 国际化适配
            val finalCountryName = runCatching {
                Locale.Builder().setRegion(city.countryCode).build().getDisplayCountry(Locale.getDefault())
            }.getOrNull()?.takeIf { it.isNotEmpty() } ?: country?.nameZh ?: country?.name ?: city.countryCode

            // 省州名字：若为中文使用内置 nameZh，其它语言查询 alternate_names
            val finalStateName = if (isZh) {
                admin1?.nameZh ?: admin1?.name ?: city.admin1Code
            } else {
                geoDao.getAlternateName("admin1", "${city.countryCode}.${city.admin1Code}", systemLang)
                    ?: admin1?.name
                    ?: city.admin1Code
            }

            // 城市名字：若为中文使用内置 nameZh，其它语言查询 alternate_names
            val finalCityName = if (isZh) {
                city.nameZh ?: city.name
            } else {
                geoDao.getAlternateName("city", city.id.toString(), systemLang)
                    ?: city.name
            }

            OfflineLocation(
                countryCode = city.countryCode,
                countryName = finalCountryName,
                stateCode = city.admin1Code,
                stateName = finalStateName,
                cityName = finalCityName,
                latitude = city.latitude,
                longitude = city.longitude,
                distanceKm = minDistance
            )
        }

    private suspend fun queryNearby(lat: Double, lng: Double, offset: Double): List<CityEntity> {
        val minLat = lat - offset
        val maxLat = lat + offset
        val minLng = lng - offset
        val maxLng = lng + offset

        return runCatching {
            // 国际日期变更线（180度经线）越界折返容错处理
            if (minLng < -180.0 || maxLng > 180.0) {
                val part1 = geoDao.findNearbyCities(minLat, maxLat, Math.max(-180.0, minLng), Math.min(180.0, maxLng))
                val part2 = if (minLng < -180.0) {
                    // 折返到东半球 180 度附近
                    geoDao.findNearbyCities(minLat, maxLat, minLng + 360.0, 180.0)
                } else {
                    // 折返到西半球 -180 度附近
                    geoDao.findNearbyCities(minLat, maxLat, -180.0, maxLng - 360.0)
                }
                // 合并并按 id 去重
                (part1 + part2).distinctBy { it.id }
            } else {
                geoDao.findNearbyCities(minLat, maxLat, minLng, maxLng)
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun performOnlineGeocode(lat: Double, lng: Double): OfflineLocation? {
        val ctx = context ?: return null
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            val addresses = geocoder.getFromLocationCompat(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val countryCode = address.countryCode ?: "CN"
                val countryName = address.countryName ?: ""
                val stateName = address.adminArea ?: ""
                val cityName = address.locality ?: address.subAdminArea ?: ""
                OfflineLocation(
                    countryCode = countryCode,
                    countryName = countryName,
                    stateCode = "",
                    stateName = stateName,
                    cityName = cityName,
                    latitude = lat,
                    longitude = lng,
                    distanceKm = 0.0
                )
            } else {
                null
            }
        }.getOrNull()
    }

    private suspend fun Geocoder.getFromLocationCompat(
        latitude: Double,
        longitude: Double,
        maxResults: Int
    ): List<Address>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                getFromLocation(latitude, longitude, maxResults, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) {
                            continuation.resume(addresses)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                })
            }
        } else {
            @Suppress("DEPRECATION")
            getFromLocation(latitude, longitude, maxResults)
        }
    }
}
