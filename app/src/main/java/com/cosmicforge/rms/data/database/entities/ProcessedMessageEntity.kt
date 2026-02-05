package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Processed Messages Entity - Client-Side Idempotency
 * 
 * Tracks all messages that have been successfully processed to prevent
 * duplicate execution in a P2P mesh network environment.
 * 
 * Security Gate Requirement: "Is double-processing prevented (Idempotency)?"
 */
@Entity(
    tableName = "processed_messages",
    indices = [
        Index(value = ["message_id"], unique = true),
        Index(value = ["processed_at"])
    ]
)
data class ProcessedMessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "message_id")
    val messageId: String, // UUID from SyncMessage
    
    @ColumnInfo(name = "message_type")
    val messageType: String, // ORDER_CREATE, ORDER_UPDATE, etc.
    
    @ColumnInfo(name = "sender_id")
    val senderId: String, // Device that sent the message
    
    @ColumnInfo(name = "processed_at")
    val processedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "checksum")
    val checksum: String, // SHA-256 for verification
    
    @ColumnInfo(name = "payload_hash")
    val payloadHash: String // Additional integrity check
)
