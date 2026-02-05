package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Dead Letter Entity - Failed Sync Vault
 * 
 * When a sync operation fails 5 times, it's moved here for manual manager review.
 * This prevents infinite retry loops and ensures the restaurant keeps running
 * even when sync issues occur.
 * 
 * Stone Tier Requirement: "Does the Shopify-style indicator turn Red when the DeadLetterVault is full?"
 * Answer: YES - UI observes unresolved count and displays red badge.
 */
@Entity(tableName = "dead_letter_vault")
data class DeadLetterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "dead_letter_id")
    val deadLetterId: Long = 0,
    
    @ColumnInfo(name = "original_message_id")
    val originalMessageId: String, // Reference to failed sync message
    
    @ColumnInfo(name = "message_type")
    val messageType: String, // Type of operation that failed
    
    @ColumnInfo(name = "payload")
    val payload: String, // Original data that failed to sync
    
    @ColumnInfo(name = "checksum")
    val checksum: String, // Original checksum
    
    @ColumnInfo(name = "failure_reason")
    val failureReason: String, // Why sync failed (server error, network timeout, etc.)
    
    @ColumnInfo(name = "failure_count")
    val failureCount: Int, // Total attempts before moving to vault (should be 5)
    
    @ColumnInfo(name = "last_error")
    val lastError: String? = null, // Last error message from server
    
    @ColumnInfo(name = "requires_manager_review")
    val requiresManagerReview: Boolean = true, // Flag for UI notification
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null, // When manager resolved this
    
    @ColumnInfo(name = "resolved_by")
    val resolvedBy: String? = null, // Manager name who resolved
    
    @ColumnInfo(name = "resolved_by_id")
    val resolvedById: Long? = null, // Manager ID
    
    @ColumnInfo(name = "resolution_notes")
    val resolutionNotes: String? = null // Manager's notes on resolution
) {
    /**
     * Check if this dead letter is still unresolved
     */
    fun isUnresolved(): Boolean = resolvedAt == null
    
    companion object {
        const val REASON_SERVER_ERROR = "SERVER_ERROR"
        const val REASON_NETWORK_TIMEOUT = "NETWORK_TIMEOUT"
        const val REASON_CHECKSUM_MISMATCH = "CHECKSUM_MISMATCH"
        const val REASON_CONFLICT_UNRESOLVABLE = "CONFLICT_UNRESOLVABLE"
    }
}
