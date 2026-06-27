package com.wkq.util.location.geo.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "geonames_cities",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["country_code", "admin1_code"])
    ]
)
data class CityEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "name_zh")
    val nameZh: String?,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "country_code")
    val countryCode: String,
    
    @ColumnInfo(name = "admin1_code")
    val admin1Code: String,
    
    @ColumnInfo(name = "population")
    val population: Int
)
