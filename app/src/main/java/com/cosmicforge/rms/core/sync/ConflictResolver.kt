package com.cosmicforge.rms.core.sync

import com.cosmicforge.rms.data.database.entities.OrderEntity

/**
 * Conflict Resolver - Hybrid Conflict Resolution Logic
 * 
 * Implements the Antigravity conflict resolution matrix:
 * 1. Additive Changes → Auto-merge
 * 2. Quantity Updates → Latest timestamp wins
 * 3. Status Conflicts → Priority ranking (Manager > Staff)
 * 
 * Mathematical Formula: Result = max(T_local, T_server) when V_local == V_server
 * 
 * Stone Tier Requirement: "Does a VOID status correctly crush a PENDING status?"
 * Answer: YES - See statusPriority map below
 */
object ConflictResolver {
    
    /**
     * Status Priority Map
     * Higher number = Higher priority
     * 
     * Stone Tier: Manager Supremacy
     * VOID and PAID (manager actions) have priority 100 and 90
     * PENDING (staff action) has priority 10
     */
    private val statusPriority = mapOf(
        "VOID" to 100,           // Manager override - highest priority
        "PAID" to 90,            // Payment completion - very high priority
        "COMPLETED" to 80,       // Order completed
        "READY" to 60,           // Ready for serving
        "IN_PROGRESS" to 50,     // Being prepared
        "PENDING" to 10          // Just created - lowest priority
    )
    
    /**
     * Resolve version conflict using versioning and timestamp
     * 
     * @param localVersion Local entity version
     * @param remoteVersion Remote entity version
     * @param localTimestamp Local update timestamp (nanoseconds)
     * @param remoteTimestamp Remote update timestamp (nanoseconds)
     * @return true if remote should be accepted, false if local should be kept
     */
    fun resolveVersionConflict(
        localVersion: Long,
        remoteVersion: Long,
        localTimestamp: Long,
        remoteTimestamp: Long
    ): Boolean {
        // If remote version is newer, accept it
        if (remoteVersion > localVersion) {
            return true
        }
        
        // If remote version is older, reject it
        if (remoteVersion < localVersion) {
            return false
        }
        
        // Versions are equal, use timestamp comparison
        // Stone Tier: Nanosecond precision prevents same-second conflicts
        return remoteTimestamp > localTimestamp
    }
    
    /**
     * Resolve status conflict using priority ranking
     * 
     * Stone Tier: Manager Supremacy
     * VOID from Manager (priority 100) beats PENDING from Staff (priority 10)
     * 
     * @param localStatus Current local status
     * @param remoteStatus Incoming remote status
     * @return The status that should win (higher priority)
     */
    fun resolveStatusConflict(
        localStatus: String,
        remoteStatus: String
    ): String {
        val localPriority = statusPriority[localStatus] ?: 0
        val remotePriority = statusPriority[remoteStatus] ?: 0
        
        return if (remotePriority > localPriority) {
            remoteStatus
        } else {
            localStatus
        }
    }
    
    /**
     * Determine if a change is additive (can be auto-merged)
     * 
     * Additive changes are operations that don't conflict:
     * - Waiter A adds Pizza to Table 4
     * - Waiter B adds Soda to Table 4
     * → Both should be kept (auto-merge)
     * 
     * @param messageType Type of sync message
     * @return true if this is an additive operation
     */
    fun isAdditiveChange(messageType: String): Boolean {
        return when (messageType) {
            "ORDER_CREATE" -> true      // New orders are always additive
            "ORDER_DETAIL_CREATE" -> true  // Adding items is additive
            "CHIEF_CLAIM" -> true       // Chef claiming items is additive
            else -> false               // Updates and deletes are not additive
        }
    }
    
    /**
     * Calculate priority for a sync message based on user role and operation
     * 
     * @param userRole Role of the user making the change (MANAGER, STAFF, CHEF)
     * @param operation Type of operation (VOID, UPDATE, CREATE)
     * @return Priority score (0-100)
     */
    fun calculateMessagePriority(userRole: String, operation: String): Int {
        val rolePriority = when (userRole.uppercase()) {
            "MANAGER" -> 100
            "CHEF" -> 50
            "STAFF" -> 10
            else -> 0
        }
        
        val operationBonus = when (operation.uppercase()) {
            "VOID" -> 50
            "PAYMENT" -> 40
            "STATUS_CHANGE" -> 20
            else -> 0
        }
        
        return minOf(100, rolePriority + operationBonus)
    }
    
    /**
     * Merge two orders with additive changes
     * This is used when both devices add items to the same order offline
     * 
     * @param local Local order
     * @param remote Remote order
     * @return Merged order (takes latest timestamps and highest priority status)
     */
    fun mergeOrders(local: OrderEntity, remote: OrderEntity): OrderEntity {
        // Use latest timestamp for updated_at
        val latestTimestamp = maxOf(local.updatedAt, remote.updatedAt)
        
        // Resolve status conflict using priority
        val resolvedStatus = resolveStatusConflict(local.status, remote.status)
        
        // Take the higher version
        val resolvedVersion = maxOf(local.version, remote.version) + 1 // Increment for merge
        
        return local.copy(
            status = resolvedStatus,
            updatedAt = latestTimestamp,
            version = resolvedVersion
        )
    }
}
