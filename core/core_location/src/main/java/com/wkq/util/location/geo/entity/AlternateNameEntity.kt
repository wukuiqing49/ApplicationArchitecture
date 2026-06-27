package com.wkq.util.location.geo.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "geonames_alternate_names",
    primaryKeys = ["entity_type", "entity_id", "lang"]
)
data class AlternateNameEntity(
    @ColumnInfo(name = "entity_type")
    val entityType: String, // "city" 或 "admin1"
    
    @ColumnInfo(name = "entity_id")
    val entityId: String,   // 城市id（数字字符）或省份key ("CN.24")
    
    @ColumnInfo(name = "lang")
    val lang: String,       // 语言代码 ("zh", "ja", "fr", "es" 等)
    
    @ColumnInfo(name = "name")
    val name: String
)
