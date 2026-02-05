package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.SyncQueueEntity
import com.cosmicforge.rms.data.database.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Sync Queue - Persistent Outbox Pattern
 */
@Dao
interface SyncQueueDao {
    
    /**
     * Get all pending messages ordered by timestamp (FIFO)
     * Stone Tier: This ensures messages are synced in the order they were created
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<SyncQueueEntity>
    
    /**
     * Observe queue size for UI feedback
     * Stone Tier: Powers the "Syncing..." pill indicator
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'SYNCING')")
    fun observeQueueSize(): Flow<Int>
    
    /**
     * Get messages that have failed and need to move to dead letter vault
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' AND retry_count >= :maxRetries")
    suspend fun getFailedMessages(maxRetries: Int = SyncQueueEntity.MAX_RETRY_COUNT): List<SyncQueueEntity>
    
    /**
     * Get a specific message by ID
     */
    @Query("SELECT * FROM sync_queue WHERE queue_id = :queueId")
    suspend fun getMessageById(queueId: Long): SyncQueueEntity?
    
    /**
     * Check if a message already exists (deduplication)
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE message_id = :messageId")
    suspend fun messageExists(messageId: String): Int
    
    /**
     * Insert a new message into the queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SyncQueueEntity): Long
    
    /**
     * Insert multiple messages (batch operation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SyncQueueEntity>)
    
    /**
     * Update message status
     */
    @Query("""
        UPDATE sync_queue 
        SET status = :status, 
            last_attempt_at = :timestamp,
            error_message = :errorMessage
        WHERE queue_id = :queueId
    """)
    suspend fun updateMessageStatus(
        queueId: Long, 
        status: SyncStatus,
        timestamp: Long = System.currentTimeMillis(),
        errorMessage: String? = null
    )
    
    /**
     * Increment retry count
     * Stone Tier: Tracks exponential backoff attempts
     */
    @Query("""
        UPDATE sync_queue 
        SET retry_count = retry_count + 1,
            last_attempt_at = :timestamp,
            error_message = :errorMessage
        WHERE queue_id = :queueId
    """)
    suspend fun incrementRetryCount(
        queueId: Long,
        timestamp: Long = System.currentTimeMillis(),
        errorMessage: String? = null
    )
    
    /**
     * Delete a message after successful sync
     */
    @Query("DELETE FROM sync_queue WHERE queue_id = :queueId")
    suspend fun deleteMessage(queueId: Long)
    
    /**
     * Delete multiple messages (batch cleanup)
     */
    @Query("DELETE FROM sync_queue WHERE queue_id IN (:queueIds)")
    suspend fun deleteMessages(queueIds: List<Long>)
    
    /**
     * Delete all synced messages (cleanup)
     */
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedMessages()
    
    /**
     * Get all messages for debugging
     */
    @Query("SELECT * FROM sync_queue ORDER BY created_at DESC")
    fun observeAllMessages(): Flow<List<SyncQueueEntity>>
    
    /**
     * Get count by status
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun getCountByStatus(status: SyncStatus): Int
}
