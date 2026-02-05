package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync Queue Entity - Persistent Outbox Pattern
 * 
 * Implements reliable offline-first sync by persisting all sync operations
 * to the database before attempting network transmission. This ensures
 * no data loss even if the app is killed mid-sync.
 * 
 * Stone Tier Requirement: "Does the order stay in SyncQueueEntity if I kill the app mid-sync?"
 * Answer: YES - All sync operations are persisted to SQLite before transmission.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "queue_id")
    val queueId: Long = 0,
    
    @ColumnInfo(name = "message_id")
    val messageId: String, // UUID for deduplication
    
    @ColumnInfo(name = "message_type")
    val messageType: String, // ORDER_CREATE, ORDER_UPDATE, etc.
    
    @ColumnInfo(name = "payload")
    val payload: String, // JSON serialized data
    
    @ColumnInfo(name = "checksum")
    val checksum: String, // SHA-256 hash for integrity verification
    
    @ColumnInfo(name = "version")
    val version: Long, // Entity version for conflict resolution
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Millisecond timestamp
    
    @ColumnInfo(name = "high_res_timestamp")
    val highResTimestamp: Long, // Nanosecond precision for conflict resolution
    
    @ColumnInfo(name = "priority")
    val priority: Int = 0, // For status conflict ranking (0=normal, 100=manager override)
    
    @ColumnInfo(name = "is_additive")
    val isAdditive: Boolean = false, // Flag for auto-merge strategy
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0, // Number of sync attempts
    
    @ColumnInfo(name = "status")
    val status: SyncStatus = SyncStatus.PENDING,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null, // Last sync attempt timestamp
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null // Last error if failed
) {
    companion object {
        const val MAX_RETRY_COUNT = 5 // Move to dead letter vault after 5 failures
        
        // Exponential backoff delays in milliseconds
        // Stone Tier Requirement: "Does the app wait longer between each try?"
        fun getRetryDelay(retryCount: Int): Long {
            return when (retryCount) {
                0 -> 100L   // First retry: 100ms
                1 -> 200L   // Second retry: 200ms
                2 -> 400L   // Third retry: 400ms
                3 -> 800L   // Fourth retry: 800ms
                4 -> 1600L  // Fifth retry: 1600ms
                else -> 3200L // Fallback (shouldn't reach here)
            }
        }
    }
}

/**
 * Sync status enumeration
 */
enum class SyncStatus {
    PENDING,    // Waiting to be synced
    SYNCING,    // Currently being transmitted
    SYNCED,     // Successfully synced
    FAILED      // Failed after retry (will move to dead letter)
}
