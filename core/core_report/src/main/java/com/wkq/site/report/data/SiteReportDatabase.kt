package com.wkq.site.report.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InspectionRecordEntity::class,
        InspectionItemEntity::class,
        InspectionPhotoEntity::class,
        AcceptanceSignatureEntity::class,
        SignatureProfileEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class SiteReportDatabase : RoomDatabase() {

    abstract fun inspectionRecordDao(): InspectionRecordDao
    abstract fun inspectionItemDao(): InspectionItemDao
    abstract fun inspectionPhotoDao(): InspectionPhotoDao
    abstract fun acceptanceSignatureDao(): AcceptanceSignatureDao
    abstract fun signatureProfileDao(): SignatureProfileDao

    companion object {
        @Volatile
        private var instance: SiteReportDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inspection_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `inspection_id` INTEGER NOT NULL,
                        `item_key` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `photo_count` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`inspection_id`) REFERENCES `inspection_record`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspection_item_inspection_id` ON `inspection_item` (`inspection_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inspection_item_inspection_id_item_key` ON `inspection_item` (`inspection_id`, `item_key`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inspection_photo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `inspection_id` INTEGER NOT NULL,
                        `item_id` INTEGER NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`inspection_id`) REFERENCES `inspection_record`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`item_id`) REFERENCES `inspection_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspection_photo_inspection_id` ON `inspection_photo` (`inspection_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspection_photo_item_id` ON `inspection_photo` (`item_id`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN required INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN min_photo_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN result TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN issue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN suggestion TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_photo ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE inspection_photo ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE inspection_photo ADD COLUMN location_accuracy REAL")
                db.execSQL("ALTER TABLE inspection_photo ADD COLUMN location_provider TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN section_key TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN section_title TEXT NOT NULL DEFAULT 'Inspection Items'")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN photo_required INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN allow_not_applicable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN require_note INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN require_issue INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN require_suggestion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN responsible_party TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspection_item ADD COLUMN rectification_deadline TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE inspection_item SET photo_required = CASE WHEN min_photo_count > 0 THEN 1 ELSE 0 END")
                db.execSQL("UPDATE inspection_item SET status = 'pass' WHERE status = 'done'")
                db.execSQL("UPDATE inspection_item SET status = 'fail' WHERE status = 'failed'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `acceptance_signature` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `inspection_id` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `signer_name` TEXT NOT NULL,
                        `company_name` TEXT NOT NULL,
                        `position` TEXT NOT NULL,
                        `signature_image_path` TEXT NOT NULL,
                        `signed_at` INTEGER NOT NULL,
                        `location_text` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`inspection_id`) REFERENCES `inspection_record`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_signature_inspection_id` ON `acceptance_signature` (`inspection_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_acceptance_signature_inspection_id_role` ON `acceptance_signature` (`inspection_id`, `role`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_record ADD COLUMN pdf_template_id TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `signature_profile` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `company_name` TEXT NOT NULL,
                        `company_address` TEXT NOT NULL,
                        `company_phone` TEXT NOT NULL,
                        `company_logo_path` TEXT NOT NULL,
                        `submitter_name` TEXT NOT NULL,
                        `submitter_role` TEXT NOT NULL,
                        `signature_image_path` TEXT NOT NULL,
                        `selected` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspection_record ADD COLUMN auto_upload_enabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE inspection_record SET auto_upload_enabled = 1 WHERE server_profile_id != ''")
            }
        }

        fun getInstance(context: Context): SiteReportDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SiteReportDatabase::class.java,
                    "site_report.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
