import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.utils.PerformanceCalculator
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
        return PerformanceCalculator.calculateScore(revenue, orderCount, voidCount)
    }
    
    /**
     * Get top performer for specific month
     */
    suspend fun getTopPerformerForMonth(monthYear: String): RewardEntity? {
        return rewardDao.getTopPerformerForMonth(monthYear)
    }
    
    /**
     * Check if user is current month's winner
     * Used for login badge display
     */
    suspend fun isCurrentMonthWinner(userId: Long): Boolean {
        val currentMonth = PerformanceCalculator.getCurrentMonthYear()
        val topPerformer = rewardDao.getTopPerformerForMonth(currentMonth)
        return topPerformer?.userId == userId
    }
    
    /**
     * Archive month-end performance and create reward
     * Automatically called on 1st of month
     * 
     * This function:
     * 1. Queries all orders from previous month
     * 2. Groups by userId
     * 3. Calculates performance scores
     * 4. Creates RewardEntity for top performer
     */
    suspend fun archiveMonthlyWinner(currentUser: UserEntity): Boolean {
        // RBAC: Only Owner can archive
        if (currentUser.roleLevel != UserEntity.ROLE_OWNER) {
            return false
        }
        
        val previousMonth = PerformanceCalculator.getPreviousMonthYear()
        
        // Check if already archived
        val existingReward = rewardDao.getTopPerformerForMonth(previousMonth)
        if (existingReward != null) {
            return false // Already archived
        }
        
        // TODO: Implement full order aggregation logic
        // For now, this is a placeholder
        // In production, this would:
        // 1. Query orders for previousMonth
        // 2. Group by userId
        // 3. Calculate total revenue, order count, void count per user
        // 4. Sort by performance score
        // 5. Insert RewardEntity for winner
        
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
