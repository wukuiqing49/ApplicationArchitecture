package com.wkq.util.location.geo.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wkq.util.location.geo.entity.Admin1Entity
import com.wkq.util.location.geo.entity.CityEntity
import com.wkq.util.location.geo.entity.CountryEntity

@Dao
interface GeoDao {

    @Query("SELECT * FROM geonames_cities WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng")
    suspend fun findNearbyCities(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<CityEntity>

    @Query("SELECT * FROM geonames_countries WHERE country_code = :countryCode LIMIT 1")
    suspend fun getCountry(countryCode: String): CountryEntity?

    @Query("SELECT * FROM geonames_admin1 WHERE country_code = :countryCode AND admin1_code = :admin1Code LIMIT 1")
    suspend fun getAdmin1(countryCode: String, admin1Code: String): Admin1Entity?

    @Query("SELECT name FROM geonames_alternate_names WHERE entity_type = :entityType AND entity_id = :entityId AND lang = :lang LIMIT 1")
    suspend fun getAlternateName(entityType: String, entityId: String, lang: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlternateNames(alternateNames: List<com.wkq.util.location.geo.entity.AlternateNameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountries(countries: List<CountryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin1s(admin1s: List<Admin1Entity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<CityEntity>)
}
