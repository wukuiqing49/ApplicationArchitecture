package com.wkq.util.location.geo

data class OfflineLocation(
    val countryCode: String,
    val countryName: String,
    val stateCode: String?,
    val stateName: String?,
    val cityName: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double
)
