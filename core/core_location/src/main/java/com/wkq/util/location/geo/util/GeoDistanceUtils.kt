package com.wkq.util.location.geo.util

import kotlin.math.*

object GeoDistanceUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * 计算两点经纬度之间的 Haversine 真实大圆距离
     */
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                cos(originLatRad) * cos(targetLatRad) * sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }
}
