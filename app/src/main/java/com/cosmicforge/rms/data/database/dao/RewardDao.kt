package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.RewardEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reward/performance tracking
 * v10.0: Gamification Engine
 */
@Dao
interface RewardDao {
    
    @Query("SELECT * FROM rewards ORDER BY created_at DESC")
    fun getAllRewards(): Flow<List<RewardEntity>>
    
    @Query("SELECT * FROM rewards WHERE month_year = :monthYear ORDER BY performance_score DESC")
    fun getRewardsByMonth(monthYear: String): Flow<List<RewardEntity>>
    
    @Query("SELECT * FROM rewards WHERE user_id = :userId ORDER BY created_at DESC")
    fun getRewardsByUser(userId: Long): Flow<List<RewardEntity>>
    
    @Query("SELECT * FROM rewards WHERE user_id = :userId AND month_year = :monthYear")
    suspend fun getRewardByUserAndMonth(userId: Long, monthYear: String): RewardEntity?
    
    @Query("SELECT * FROM rewards WHERE month_year = :monthYear ORDER BY performance_score DESC LIMIT 1")
    suspend fun getTopPerformerForMonth(monthYear: String): RewardEntity?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReward(reward: RewardEntity): Long
    
    @Update
    suspend fun updateReward(reward: RewardEntity)
    
    @Query("UPDATE rewards SET is_paid = :isPaid WHERE reward_id = :rewardId")
    suspend fun markAsPaid(rewardId: Long, isPaid: Boolean)
    
    @Delete
    suspend fun deleteReward(reward: RewardEntity)
}
