package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.data.database.dao.OrderDao
import com.cosmicforge.rms.data.database.dao.SyncQueueDao
import com.cosmicforge.rms.data.database.dao.UserDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for system diagnostics and vault health
 * v10.3.1: Diagnostics & Monitoring
 */
@Singleton
class DiagnosticsRepository @Inject constructor(
    private val userDao: UserDao,
    private val orderDao: OrderDao,
    private val syncQueueDao: SyncQueueDao
) {
    
    /**
     * Get total user count
     */
    suspend fun getUserCount(): Int {
        return userDao.getAllUsers().first().size
    }
    
    /**
     * Get active user count
     */
    suspend fun getActiveUserCount(): Int {
        return userDao.getAllActiveUsers().first().size
    }
    
    /**
     * Get total order count
     */
    suspend fun getOrderCount(): Int {
        return orderDao.getAllOrders().first().size
    }
    
    /**
     * Get pending sync count
     */
    suspend fun getPendingSyncCount(): Int {
        return syncQueueDao.getPendingSyncItems().first().size
    }
    
    /**
     * Get active device count (Stone Vault mesh)
     * For Silver Tier: Max 8 devices
     */
    suspend fun getActiveDeviceCount(): Int {
        // TODO: Implement device tracking via SyncLog
        // For now, return 1 (current device)
        return 1
    }
    
    /**
     * Get subscription tier status
     */
    fun getSubscriptionTier(): SubscriptionTier {
        return SubscriptionTier.SILVER // Hardcoded for v10
    }
    
    /**
     * Check if at capacity
     */
    suspend fun isAtCapacity(): Boolean {
        val tier = getSubscriptionTier()
        val deviceCount = getActiveDeviceCount()
        return deviceCount >= tier.maxDevices
    }
}

/**
 * Subscription tiers for Cosmic Forge POS
 */
enum class SubscriptionTier(
    val tierName: String,
    val maxDevices: Int
) {
    SILVER("Silver", 8),
    GOLD("Gold", 16),
    PLATINUM("Platinum", 32)
}
