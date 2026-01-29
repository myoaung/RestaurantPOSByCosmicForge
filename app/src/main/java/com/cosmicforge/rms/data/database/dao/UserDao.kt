package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY user_name")
    fun getAllActiveUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?
    
    @Query("SELECT * FROM users WHERE user_name = :userName AND is_active = 1")
    suspend fun getUserByName(userName: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE role_level = :roleLevel AND is_active = 1")
    fun getUsersByRole(roleLevel: Int): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE station_id = :stationId AND is_active = 1")
    fun getUsersByStation(stationId: String): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateUser(userId: Long)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
}
