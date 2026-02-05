package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.DeadLetterEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Dead Letter Vault - Failed Sync Management
 */
@Dao
interface DeadLetterDao {
    
    /**
     * Get all unresolved dead letters
     * Stone Tier: Powers the red badge on the manager UI
     */
    @Query("SELECT * FROM dead_letter_vault WHERE resolved_at IS NULL ORDER BY created_at DESC")
    suspend fun getUnresolvedItems(): List<DeadLetterEntity>
    
    /**
     * Observe unresolved count for UI badge
     * Stone Tier: "Does the Shopify-style indicator turn Red when the DeadLetterVault is full?"
     */
    @Query("SELECT COUNT(*) FROM dead_letter_vault WHERE resolved_at IS NULL")
    fun observeUnresolvedCount(): Flow<Int>
    
    /**
     * Get all dead letters (including resolved)
     */
    @Query("SELECT * FROM dead_letter_vault ORDER BY created_at DESC")
    fun observeAllDeadLetters(): Flow<List<DeadLetterEntity>>
    
    /**
     * Get a specific dead letter by ID
     */
    @Query("SELECT * FROM dead_letter_vault WHERE dead_letter_id = :deadLetterId")
    suspend fun getDeadLetterById(deadLetterId: Long): DeadLetterEntity?
    
    /**
     * Insert a failed message into the vault
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadLetter(deadLetter: DeadLetterEntity): Long
    
    /**
     * Mark a dead letter as resolved by manager
     */
    @Query("""
        UPDATE dead_letter_vault 
        SET resolved_at = :resolvedAt,
            resolved_by = :resolvedBy,
            resolved_by_id = :resolvedById,
            resolution_notes = :resolutionNotes,
            requires_manager_review = 0
        WHERE dead_letter_id = :deadLetterId
    """)
    suspend fun markAsResolved(
        deadLetterId: Long,
        resolvedAt: Long = System.currentTimeMillis(),
        resolvedBy: String,
        resolvedById: Long,
        resolutionNotes: String? = null
    )
    
    /**
     * Delete a dead letter (permanent removal)
     */
    @Query("DELETE FROM dead_letter_vault WHERE dead_letter_id = :deadLetterId")
    suspend fun deleteDeadLetter(deadLetterId: Long)
    
    /**
     * Delete all resolved dead letters older than specified time
     * (Cleanup for old resolved items)
     */
    @Query("DELETE FROM dead_letter_vault WHERE resolved_at IS NOT NULL AND resolved_at < :olderThan")
    suspend fun deleteResolvedOlderThan(olderThan: Long)
    
    /**
     * Get dead letters by message type
     */
    @Query("SELECT * FROM dead_letter_vault WHERE message_type = :messageType ORDER BY created_at DESC")
    suspend fun getByMessageType(messageType: String): List<DeadLetterEntity>
    
    /**
     * Get dead letters by failure reason
     */
    @Query("SELECT * FROM dead_letter_vault WHERE failure_reason = :reason ORDER BY created_at DESC")
    suspend fun getByFailureReason(reason: String): List<DeadLetterEntity>
}
