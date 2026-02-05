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
 * Version 9: Enhanced Antigravity with Idempotency
 * - SyncQueueEntity: Persistent outbox pattern for offline-first sync
 * - DeadLetterEntity: Failed sync vault for manager review
 * - ProcessedMessageEntity: Client-side idempotency tracking (Security Gate)
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
        SyncQueueEntity::class,      // NEW: Persistent sync queue
        DeadLetterEntity::class,      // NEW: Dead letter vault
        ProcessedMessageEntity::class // NEW: Idempotency tracking (Security Gate)
    ],
    version = 9,
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
    abstract fun syncQueueDao(): SyncQueueDao           // NEW: Sync queue DAO
    abstract fun deadLetterDao(): DeadLetterDao         // NEW: Dead letter DAO
    abstract fun processedMessagesDao(): ProcessedMessagesDao // NEW: Idempotency DAO
    
    companion object {
        const val DATABASE_NAME = "cosmic_forge_db"
        const val DATABASE_VERSION = 9
    }
}
