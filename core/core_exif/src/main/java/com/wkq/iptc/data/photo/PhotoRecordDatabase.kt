package com.wkq.iptc.data.photo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhotoRecordEntity::class], version = 5, exportSchema = false)
abstract class PhotoRecordDatabase : RoomDatabase() {

    abstract fun photoRecordDao(): PhotoRecordDao

    companion object {
        @Volatile
        private var instance: PhotoRecordDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photo_record ADD COLUMN photo_source TEXT NOT NULL DEFAULT 'batch_processing'")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photo_record ADD COLUMN writer TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photo_record ADD COLUMN anti_tamper_signed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photo_record ADD COLUMN account_id TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN source_uri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN source_key TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN process_type TEXT NOT NULL DEFAULT 'legacy'")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN output_path TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN output_mime_type TEXT NOT NULL DEFAULT 'image/jpeg'")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE photo_record ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'local_only'")
                db.execSQL("UPDATE photo_record SET source_uri = uri WHERE source_uri = ''")
                db.execSQL("UPDATE photo_record SET source_key = 'legacy_' || id WHERE source_key = ''")
                db.execSQL("UPDATE photo_record SET updated_at = COALESCE(processed_at, captured_at, 0) WHERE updated_at = 0")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_photo_record_account_id_source_key_process_type " +
                        "ON photo_record(account_id, source_key, process_type)"
                )
            }
        }

        fun getInstance(context: Context): PhotoRecordDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhotoRecordDatabase::class.java,
                    "press_iptc_photo_record.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
