package com.cosmicforge.rms.di

import android.content.Context
import androidx.room.Room
import com.cosmicforge.rms.data.database.CosmicForgeDatabase
import com.cosmicforge.rms.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Hilt module for database dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CosmicForgeDatabase {
        // Generate encryption key from device-specific info
        // In production, this should be generated securely and stored in Android Keystore
        val passphrase = SQLiteDatabase.getBytes("CosmicForge2024!".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            CosmicForgeDatabase::class.java,
            CosmicForgeDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory) // Enable SQLCipher encryption
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10) // Add HR migration
            .build()
    }
    
    /**
     * Migration from version 7 to 8
     * Adds Antigravity Sync Protocol tables
     * 
     * Stone Tier Requirement: "Migration Risk - ensure old orders aren't wiped"
     * This migration ONLY adds new tables, existing data is preserved
     */
    private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Create sync_queue table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_queue (
                    queue_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    message_id TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    checksum TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    high_res_timestamp INTEGER NOT NULL,
                    priority INTEGER NOT NULL DEFAULT 0,
                    is_additive INTEGER NOT NULL DEFAULT 0,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    last_attempt_at INTEGER,
                    error_message TEXT
                )
            """.trimIndent())
            
            // Create index on status for faster queue queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_sync_queue_status 
                ON sync_queue(status)
            """.trimIndent())
            
            // Create index on message_id for deduplication
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_sync_queue_message_id 
                ON sync_queue(message_id)
            """.trimIndent())
            
            // Create dead_letter_vault table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS dead_letter_vault (
                    dead_letter_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    original_message_id TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    checksum TEXT NOT NULL,
                    failure_reason TEXT NOT NULL,
                    failure_count INTEGER NOT NULL,
                    last_error TEXT,
                    requires_manager_review INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    resolved_at INTEGER,
                    resolved_by TEXT,
                    resolved_by_id INTEGER,
                    resolution_notes TEXT
                )
            """.trimIndent())
            
            // Create index on resolved_at for unresolved queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_dead_letter_vault_resolved 
                ON dead_letter_vault(resolved_at)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 8 to 9
     * Adds Processed Messages table for idempotency (Security Gate)
     * 
     * Security Gate Requirement: "Is double-processing prevented?"
     */
    private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Create processed_messages table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS processed_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    message_id TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    sender_id TEXT NOT NULL,
                    processed_at INTEGER NOT NULL,
                    checksum TEXT NOT NULL,
                    payload_hash TEXT NOT NULL
                )
            """.trimIndent())
            
            // Create unique index on message_id (CRITICAL for idempotency)
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS index_processed_messages_message_id 
                ON processed_messages(message_id)
            """.trimIndent())
            
            // Create index on processed_at for cleanup queries
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_processed_messages_processed_at 
                ON processed_messages(processed_at)
            """.trimIndent())
        }
    }
    
    /**
     * Migration from version 9 to 10
     * Adds Complete HR Management System tables
     * 
     * HR Requirements:
     * - RewardEntity: Performance tracking and gamification
     * - BlacklistEntity: NRC-based hiring protection
     * - LeaveRequestEntity: Time-off management with approval workflow
     */
    private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Create rewards table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS rewards (
                    reward_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    month_year TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    performance_score REAL NOT NULL,
                    total_revenue REAL NOT NULL,
                    order_count INTEGER NOT NULL,
                    void_count INTEGER NOT NULL,
                    is_paid INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create blacklist table with unique NRC constraint
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS blacklist (
                    blacklist_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nrc_number TEXT NOT NULL,
                    staff_name TEXT NOT NULL,
                    reason_for_blacklist TEXT NOT NULL,
                    blacklisted_at INTEGER NOT NULL,
                    blacklisted_by INTEGER NOT NULL
                )
            """.trimIndent())
            
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS index_blacklist_nrc_number 
                ON blacklist(nrc_number)
            """.trimIndent())
            
            // Create leave_requests table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS leave_requests (
                    request_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    user_id INTEGER NOT NULL,
                    leave_type TEXT NOT NULL,
                    start_date INTEGER NOT NULL,
                    end_date INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Pending',
                    reviewed_by INTEGER,
                    reviewed_at INTEGER,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }
    
    @Provides
    fun provideShopConfigDao(database: CosmicForgeDatabase): ShopConfigDao {
        return database.shopConfigDao()
    }
    
    @Provides
    fun provideUserDao(database: CosmicForgeDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideTableDao(database: CosmicForgeDatabase): TableDao {
        return database.tableDao()
    }
    
    @Provides
    fun provideMenuItemDao(database: CosmicForgeDatabase): MenuItemDao {
        return database.menuItemDao()
    }
    
    @Provides
    fun provideOrderDao(database: CosmicForgeDatabase): OrderDao {
        return database.orderDao()
    }
    
    @Provides
    fun provideOrderDetailDao(database: CosmicForgeDatabase): OrderDetailDao {
        return database.orderDetailDao()
    }
    
    @Provides
    fun provideSecurityAuditDao(database: CosmicForgeDatabase): SecurityAuditDao {
        return database.securityAuditDao()
    }
    
    
    @Provides
    fun provideSMSTemplateDao(database: CosmicForgeDatabase): SMSTemplateDao {
        return database.smsTemplateDao()
    }
    
    @Provides
    fun provideSyncQueueDao(database: CosmicForgeDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
    
    @Provides
    fun provideDeadLetterDao(database: CosmicForgeDatabase): DeadLetterDao {
        return database.deadLetterDao()
    }
    
    @Provides
    fun provideProcessedMessagesDao(database: CosmicForgeDatabase): ProcessedMessagesDao {
        return database.processedMessagesDao()
    }
    
    // v10: HR Management DAOs
    
    @Provides
    fun provideRewardDao(database: CosmicForgeDatabase): RewardDao {
        return database.rewardDao()
    }
    
    @Provides
    fun provideBlacklistDao(database: CosmicForgeDatabase): BlacklistDao {
        return database.blacklistDao()
    }
    
    @Provides
    fun provideLeaveRequestDao(database: CosmicForgeDatabase): LeaveRequestDao {
        return database.leaveRequestDao()
    }
}
