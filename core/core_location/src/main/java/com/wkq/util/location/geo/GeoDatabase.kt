package com.wkq.util.location.geo

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wkq.util.location.geo.dao.GeoDao
import com.wkq.util.location.geo.entity.Admin1Entity
import com.wkq.util.location.geo.entity.CityEntity
import com.wkq.util.location.geo.entity.CountryEntity

@Database(
    entities = [
        CountryEntity::class, 
        Admin1Entity::class, 
        CityEntity::class, 
        com.wkq.util.location.geo.entity.AlternateNameEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GeoDatabase : RoomDatabase() {
    abstract fun geoDao(): GeoDao
}
