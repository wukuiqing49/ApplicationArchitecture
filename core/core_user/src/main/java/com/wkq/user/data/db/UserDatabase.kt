package com.wkq.user.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wkq.user.data.dao.UserDao
import com.wkq.user.data.entity.UserEntity

/**
 * UserDatabase：用户本地数据库 (Room 实现)
 */
@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
internal abstract class UserDatabase : RoomDatabase() {

    /** 获取用户数据操作接口 */
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null
        // 升级 模板代码（Room + Migration）

        // 1: version=1 -->version=2
        //2: 创建val MIGRATION_1_2=Migration(1,2)
        //3: .addMigrations(MIGRATION_1_2)

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // -----------------------------
                // 1. 新增字段（简单）
                // -----------------------------
                db.execSQL(
                    """
            ALTER TABLE UserEntity 
            ADD COLUMN age INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )

                // -----------------------------
                // 2. 如果要新增表
                // -----------------------------
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS UserProfile (
                id INTEGER PRIMARY KEY NOT NULL,
                userId INTEGER NOT NULL,
                nickname TEXT,
                avatar TEXT
            )
            """.trimIndent()
                )

                // -----------------------------
                // 3. 复杂表修改（改字段名/删字段/改类型）
                // -----------------------------
                // 举例：UserEntity 表删掉 name 字段
                db.execSQL(
                    """
            CREATE TABLE UserEntity_new (
                id INTEGER PRIMARY KEY NOT NULL,
                age INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO UserEntity_new (id, age)
            SELECT id, age FROM UserEntity
            """.trimIndent()
                )

                db.execSQL("DROP TABLE UserEntity")
                db.execSQL("ALTER TABLE UserEntity_new RENAME TO UserEntity")
            }
        }

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, UserDatabase::class.java, "multi_user_db"
                ).fallbackToDestructiveMigration(true)
//                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
