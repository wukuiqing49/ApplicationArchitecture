package com.wkq.util.location.geo.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "geonames_admin1",
    primaryKeys = ["country_code", "admin1_code"]
)
data class Admin1Entity(
    @ColumnInfo(name = "country_code")
    val countryCode: String,
    
    @ColumnInfo(name = "admin1_code")
    val admin1Code: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "name_zh")
    val nameZh: String?
)
