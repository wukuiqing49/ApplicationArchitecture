package com.wkq.site.report.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SignatureProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: SignatureProfileEntity): Long

    @Update
    suspend fun update(profile: SignatureProfileEntity)

    @Query("SELECT * FROM signature_profile ORDER BY selected DESC, updated_at DESC")
    suspend fun queryAll(): List<SignatureProfileEntity>

    @Query("SELECT * FROM signature_profile WHERE id = :id LIMIT 1")
    suspend fun queryById(id: Long): SignatureProfileEntity?

    @Query("SELECT * FROM signature_profile WHERE selected = 1 ORDER BY updated_at DESC LIMIT 1")
    suspend fun querySelected(): SignatureProfileEntity?

    @Query("UPDATE signature_profile SET selected = 0")
    suspend fun clearSelected()

    @Query("UPDATE signature_profile SET selected = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markSelected(id: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM signature_profile")
    suspend fun countAll(): Int
}
