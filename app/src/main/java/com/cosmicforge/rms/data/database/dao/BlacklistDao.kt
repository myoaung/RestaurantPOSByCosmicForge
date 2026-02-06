package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.BlacklistEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for NRC blacklist management
 * v10.5: Security & HR Management
 */
@Dao
interface BlacklistDao {
    
    @Query("SELECT * FROM blacklist ORDER BY blacklisted_at DESC")
    fun getAllBlacklisted(): Flow<List<BlacklistEntity>>
    
    @Query("SELECT * FROM blacklist WHERE nrc_number = :nrc")
    suspend fun checkNRC(nrc: String): BlacklistEntity?
    
    @Query("SELECT * FROM blacklist WHERE blacklist_id = :id")
    suspend fun getBlacklistById(id: Long): BlacklistEntity?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBlacklist(blacklist: BlacklistEntity): Long
    
    @Update
    suspend fun updateBlacklist(blacklist: BlacklistEntity)
    
    @Delete
    suspend fun deleteBlacklist(blacklist: BlacklistEntity)
    
    @Query("DELETE FROM blacklist WHERE nrc_number = :nrc")
    suspend fun removeFromBlacklist(nrc: String)
}
