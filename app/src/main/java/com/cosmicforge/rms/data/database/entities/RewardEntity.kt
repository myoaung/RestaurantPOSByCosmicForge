package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reward tracking entity for staff performance gamification
 * Stores monthly top performer data for incentive program
 * v10.0: Gamification Engine
 */
@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "reward_id")
    val rewardId: Long = 0,
    
    @ColumnInfo(name = "month_year")
    val monthYear: String, // Format: "2026-02"
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "performance_score")
    val performanceScore: Double,
    
    @ColumnInfo(name = "total_revenue")
    val totalRevenue: Double,
    
    @ColumnInfo(name = "order_count")
    val orderCount: Int,
    
    @ColumnInfo(name = "void_count")
    val voidCount: Int,
    
    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
