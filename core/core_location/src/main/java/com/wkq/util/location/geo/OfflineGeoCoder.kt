package com.wkq.util.location.geo

interface OfflineGeoCoder {

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): OfflineLocation?
}
