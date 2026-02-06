package com.cosmicforge.rms.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmicforge.rms.data.database.dao.*
import com.cosmicforge.rms.data.database.entities.*

/**
 * Main Room Database for Cosmic Forge RMS
 * Uses SQLCipher for encryption
 * 
 * Version 10: Complete HR Management System
 * - RewardEntity: Staff performance tracking and gamification
 * - BlacklistEntity: NRC-based hiring protection
 * - LeaveRequestEntity: Time-off management with approval workflow
 */
@Database(
    entities = [
        ShopConfigEntity::class,
        UserEntity::class,
        TableEntity::class,
        MenuItemEntity::class,
        OrderEntity::class,
        OrderDetailEntity::class,
        SecurityAuditEntity::class,
        SMSTemplateEntity::class,
        SyncQueueEntity::class,
        DeadLetterEntity::class,
        ProcessedMessageEntity::class,
        RewardEntity::class,          // v10: Performance tracking
        BlacklistEntity::class,       // v10: Hiring protection
        LeaveRequestEntity::class     // v10: Leave management
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class CosmicForgeDatabase : RoomDatabase() {
    
    abstract fun shopConfigDao(): ShopConfigDao
    abstract fun userDao(): UserDao
    abstract fun tableDao(): TableDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun orderDetailDao(): OrderDetailDao
    abstract fun securityAuditDao(): SecurityAuditDao
    abstract fun smsTemplateDao(): SMSTemplateDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun deadLetterDao(): DeadLetterDao
    abstract fun processedMessagesDao(): ProcessedMessagesDao
    abstract fun rewardDao(): RewardDao                 // v10: Reward DAO
    abstract fun blacklistDao(): BlacklistDao           // v10: Blacklist DAO
    abstract fun leaveRequestDao(): LeaveRequestDao     // v10: Leave DAO
    
    companion object {
        const val DATABASE_NAME = "cosmic_forge_db"
        const val DATABASE_VERSION = 10
    }
}
