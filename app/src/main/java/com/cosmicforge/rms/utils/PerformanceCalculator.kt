package com.cosmicforge.rms.utils

import com.cosmicforge.rms.data.database.entities.OrderEntity
import com.cosmicforge.rms.data.database.entities.RewardEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Performance calculation utilities for gamification
 * v10.4: Gamification Engine
 */
object PerformanceCalculator {
    
    /**
     * Calculate performance score using weighted formula
     * Formula: (Revenue * 0.6) + (OrderCount * 0.4) - (VoidCount * 5.0)
     * 
     * Example:
     * - Revenue: K500,000 → 300,000 points
     * - Orders: 100 → 40 points
     * - Voids: 2 → -10 points
     * = 299,030 points
     */
    fun calculateScore(
        totalRevenue: Double,
        orderCount: Int,
        voidCount: Int
    ): Double {
        return (totalRevenue * 0.6) + (orderCount * 0.4) - (voidCount * 5.0)
    }
    
    /**
     * Get current month-year string
     * Format: "2026-02"
     */
    fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }
    
    /**
     * Get previous month-year string
     * Used for archiving last month's winner
     */
    fun getPreviousMonthYear(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(calendar.time)
    }
    
    /**
     * Check if today is the 1st of the month (archiver trigger)
     */
    fun isFirstDayOfMonth(): Boolean {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_MONTH) == 1
    }
    
    /**
     * Create reward entity from performance data
     */
    fun createRewardEntity(
        userId: Long,
        monthYear: String,
        totalRevenue: Double,
        orderCount: Int,
        voidCount: Int
    ): RewardEntity {
        val score = calculateScore(totalRevenue, orderCount, voidCount)
        
        return RewardEntity(
            monthYear = monthYear,
            userId = userId,
            performanceScore = score,
            totalRevenue = totalRevenue,
            orderCount = orderCount,
            voidCount = voidCount,
            isPaid = false
        )
    }
}
