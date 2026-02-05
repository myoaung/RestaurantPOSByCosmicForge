package com.cosmicforge.rms.core.network.model

import java.util.UUID

/**
 * Represents a connected peer device in the mesh network
 */
data class PeerDevice(
    val deviceId: String = UUID.randomUUID().toString(),
    val deviceName: String,
    val deviceAddress: String, // MAC address or IP
    val connectionType: ConnectionType,
    val isGroupOwner: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false
)

/**
 * Connection type for fallback support
 */
enum class ConnectionType {
    WIFI_DIRECT,    // Direct P2P connection
    NSD_LOCAL,      // Network Service Discovery on local network
    DISCONNECTED
}

/**
 * Sync message payload - Enhanced for Antigravity Protocol
 */
data class SyncMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val messageType: MessageType,
    val payload: String, // JSON serialized data
    val timestamp: Long = System.currentTimeMillis(),
    val version: Long = 1,
    val checksum: String? = null, // SHA-256 hash for integrity
    val highResTimestamp: Long = System.nanoTime(), // Nanosecond precision
    val priority: Int = 0 // For status conflict ranking
)

/**
 * Types of sync messages
 */
enum class MessageType {
    ORDER_CREATE,
    ORDER_UPDATE,
    ORDER_DELETE,
    ORDER_DETAIL_UPDATE,
    TABLE_STATUS_UPDATE,
    CHIEF_CLAIM,
    MENU_UPDATE,
    HEARTBEAT,
    ACK
}

/**
 * Sync status for tracking
 */
data class SyncStatus(
    val lastSyncTimestamp: Long = 0,
    val pendingSyncCount: Int = 0,
    val connectedPeers: Int = 0,
    val syncErrors: Int = 0
)
