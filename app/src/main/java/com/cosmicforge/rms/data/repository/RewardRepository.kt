package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.data.database.dao.OrderDao
import com.cosmicforge.rms.data.database.dao.RewardDao
import com.cosmicforge.rms.data.database.entities.RewardEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for performance tracking and rewards
 * v10.0: Gamification Engine
 */
@Singleton
class RewardRepository @Inject constructor(
    private val rewardDao: RewardDao,
    private val orderDao: OrderDao
) {
    
    /**
     * Get all rewards (Manager/Owner only)
     */
    fun getAllRewards(currentUser: UserEntity): Flow<List<RewardEntity>>? {
        // RBAC: Only Manager and Owner can view all rewards
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null // Staff shielded
        }
        return rewardDao.getAllRewards()
    }
    
    /**
     * Get rewards for specific month (Manager/Owner only)
     */
    fun getRewardsByMonth(monthYear: String, currentUser: UserEntity): Flow<List<RewardEntity>>? {
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null
        }
        return rewardDao.getRewardsByMonth(monthYear)
    }
    
    /**
     * Get user's own rewards (All roles)
     */
    fun getMyRewards(userId: Long): Flow<List<RewardEntity>> {
        return rewardDao.getRewardsByUser(userId)
    }
    
    /**
     * Calculate performance score
     * Formula: (Revenue * 0.6) + (OrderCount * 0.4) - (VoidCount * 5.0)
     */
    fun calculatePerformanceScore(
        revenue: Double,
        orderCount: Int,
        voidCount: Int
    ): Double {
        return (revenue * 0.6) + (orderCount * 0.4) - (voidCount * 5.0)
    }
    
    /**
     * Get top performer for specific month
     */
    suspend fun getTopPerformerForMonth(monthYear: String): RewardEntity? {
        return rewardDao.getTopPerformerForMonth(monthYear)
    }
    
    /**
     * Archive month-end performance and create reward
     * Automatically called at end of month
     */
    suspend fun closeMonthAndArchive(monthYear: String, currentUser: UserEntity): Boolean {
        // RBAC: Only Owner can close month
        if (currentUser.roleLevel != UserEntity.ROLE_OWNER) {
            return false
        }
        
        // TODO: Calculate performance for all users
        // For now, this is a placeholder
        // In production, this would:
        // 1. Query all orders for the month
        // 2. Group by userId
        // 3. Calculate scores
        // 4. Create RewardEntity for top performer
        
        return true
    }
    
    /**
     * Mark reward as paid (Owner only)
     */
    suspend fun markAsPaid(rewardId: Long, currentUser: UserEntity): Boolean {
        // RBAC: Only Owner can mark as paid
        if (currentUser.roleLevel != UserEntity.ROLE_OWNER) {
            return false
        }
        
        rewardDao.markAsPaid(rewardId, true)
        return true
    }
}
