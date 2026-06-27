package com.wkq.util.location.geo.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geonames_countries")
data class CountryEntity(
    @PrimaryKey
    @ColumnInfo(name = "country_code")
    val countryCode: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "name_zh")
    val nameZh: String?
)
