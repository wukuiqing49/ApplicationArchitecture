package com.wkq.util.location.geo.repository

import android.content.Context
import androidx.room.Room
import com.wkq.util.location.geo.GeoDatabase
import com.wkq.util.location.geo.OfflineGeoCoder
import com.wkq.util.location.geo.OfflineGeoCoderImpl
import android.util.Log

object GeoRepository {
    private const val TAG = "GeoRepository"
    private const val DB_NAME = "geonames.db"

    @Volatile
    private var database: GeoDatabase? = null

    @Volatile
    private var geoCoder: OfflineGeoCoder? = null

    fun init(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    Log.d(TAG, "Initializing GeoNames database from asset...")
                    database = Room.databaseBuilder(
                        context.applicationContext,
                        GeoDatabase::class.java,
                        DB_NAME
                    ).createFromAsset(DB_NAME)
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()

                    geoCoder = OfflineGeoCoderImpl(database!!.geoDao(), context.applicationContext)
                    Log.d(TAG, "GeoNames database initialization triggered successfully.")
                }
            }
        }
    }

    fun getGeoCoder(context: Context): OfflineGeoCoder {
        init(context)
        return geoCoder ?: synchronized(this) {
            geoCoder ?: OfflineGeoCoderImpl(database!!.geoDao(), context.applicationContext).also { geoCoder = it }
        }
    }

    fun close() {
        synchronized(this) {
            database?.close()
            database = null
            geoCoder = null
        }
    }
}
