package com.wkq.user.data.dao

import androidx.room.*
import com.wkq.user.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据库操作接口
 */
@Dao
interface UserDao {

    /**
     * 插入或更新用户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    /**
     * 查询所有用户
     */
    @Query("SELECT * FROM user_accounts")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    /**
     * 查询当前激活用户
     */
    @Query("SELECT * FROM user_accounts WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * 查询当前激活用户（挂起函数）
     */
    @Query("SELECT * FROM user_accounts WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    /**
     * 根据 ID 查询用户
     */
    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * 删除用户
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * 切换账号：
     * 1. 将所有账号设为非当前
     * 2. 将指定 ID 账号设为当前
     */
    @Transaction
    suspend fun switchAccount(userId: String) {
        clearCurrentStatus()
        setCurrentStatus(userId)
    }

    @Query("UPDATE user_accounts SET isCurrent = 0")
    suspend fun clearCurrentStatus()

    @Query("UPDATE user_accounts SET isCurrent = 1 WHERE userId = :userId")
    suspend fun setCurrentStatus(userId: String)
}
