package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Security audit log for tracking sensitive operations
 * Logs all override actions (voids, refunds, price changes)
 */
@Entity(tableName = "security_audit")
data class SecurityAuditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "audit_id")
    val auditId: Long = 0,
    
    @ColumnInfo(name = "action_type")
    val actionType: String, // VOID, REFUND, PRICE_OVERRIDE, LOGIN, LOGOUT, etc.
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "role_level")
    val roleLevel: Int,
    
    @ColumnInfo(name = "target_type")
    val targetType: String? = null, // ORDER, ORDER_DETAIL, TABLE, MENU_ITEM
    
    @ColumnInfo(name = "target_id")
    val targetId: String? = null, // ID of the affected entity
    
    @ColumnInfo(name = "action_details")
    val actionDetails: String? = null, // JSON with specific details
    
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    
    @ColumnInfo(name = "ip_address")
    val ipAddress: String? = null,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "was_successful")
    val wasSuccessful: Boolean = true,
    
    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null
) {
    companion object {
        // Action types
        const val ACTION_LOGIN = "LOGIN"
        const val ACTION_LOGOUT = "LOGOUT"
        const val ACTION_VOID_ITEM = "VOID_ITEM"
        const val ACTION_VOID_ORDER = "VOID_ORDER"
        const val ACTION_REFUND = "REFUND"
        const val ACTION_PRICE_OVERRIDE = "PRICE_OVERRIDE"
        const val ACTION_DISCOUNT_OVERRIDE = "DISCOUNT_OVERRIDE"
        const val ACTION_MANAGER_OVERRIDE = "MANAGER_OVERRIDE"
        const val ACTION_ACCESS_DENIED = "ACCESS_DENIED"
        const val ACTION_SESSION_TIMEOUT = "SESSION_TIMEOUT"
        
        // Target types
        const val TARGET_ORDER = "ORDER"
        const val TARGET_ORDER_DETAIL = "ORDER_DETAIL"
        const val TARGET_TABLE = "TABLE"
        const val TARGET_MENU_ITEM = "MENU_ITEM"
        const val TARGET_USER = "USER"
    }
}
