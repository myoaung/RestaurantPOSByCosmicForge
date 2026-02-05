package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.ProcessedMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Processed Messages - Idempotency Enforcement
 * 
 * Security Gate: Prevents double-processing of sync messages
 */
@Dao
interface ProcessedMessagesDao {
    
    /**
     * Check if a message has already been processed
     * Security Gate: CRITICAL - Must return true for duplicates
     */
    @Query("SELECT COUNT(*) FROM processed_messages WHERE message_id = :messageId")
    suspend fun isProcessed(messageId: String): Int
    
    /**
     * Mark a message as processed
     * Must be called ONLY after successful business logic execution
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markAsProcessed(message: ProcessedMessageEntity): Long
    
    /**
     * Get all processed messages (for debugging)
     */
    @Query("SELECT * FROM processed_messages ORDER BY processed_at DESC LIMIT :limit")
    suspend fun getRecentProcessed(limit: Int = 100): List<ProcessedMessageEntity>
    
    /**
     * Observe processed message count (for metrics)
     */
    @Query("SELECT COUNT(*) FROM processed_messages")
    fun observeProcessedCount(): Flow<Int>
    
    /**
     * Clean up old processed messages (older than 30 days)
     * Prevents table from growing indefinitely
     */
    @Query("DELETE FROM processed_messages WHERE processed_at < :cutoffTimestamp")
    suspend fun cleanupOldMessages(cutoffTimestamp: Long)
    
    /**
     * Get processed messages by sender (for debugging)
     */
    @Query("SELECT * FROM processed_messages WHERE sender_id = :senderId ORDER BY processed_at DESC")
    suspend fun getProcessedBySender(senderId: String): List<ProcessedMessageEntity>
    
    /**
     * Verify checksum matches for a processed message
     * Additional integrity check
     */
    @Query("SELECT checksum FROM processed_messages WHERE message_id = :messageId")
    suspend fun getChecksumForMessage(messageId: String): String?
}
