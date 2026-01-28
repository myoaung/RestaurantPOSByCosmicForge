package com.cosmicforge.pos.core.security

import android.util.Log
import com.cosmicforge.pos.data.database.dao.SecurityAuditDao
import com.cosmicforge.pos.data.database.entities.SecurityAuditEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit logger for security-sensitive operations
 * All manager overrides, voids, and access control actions are logged
 */
@Singleton
class AuditLogger @Inject constructor(
    private val securityAuditDao: SecurityAuditDao,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Log a security action
     */
    fun logAction(
        actionType: String,
        userId: Long,
        userName: String,
        roleLevel: Int,
        targetType: String? = null,
        targetId: String? = null,
        actionDetails: String? = null,
        wasSuccessful: Boolean = true,
        failureReason: String? = null
    ) {
        scope.launch {
            try {
                val audit = SecurityAuditEntity(
                    actionType = actionType,
                    userId = userId,
                    userName = userName,
                    roleLevel = roleLevel,
                    targetType = targetType,
                    targetId = targetId,
                    actionDetails = actionDetails,
                    deviceId = deviceInfoProvider.getDeviceId(),
                    ipAddress = deviceInfoProvider.getIpAddress(),
                    timestamp = System.currentTimeMillis(),
                    wasSuccessful = wasSuccessful,
                    failureReason = failureReason
                )
                
                securityAuditDao.insertAudit(audit)
                Log.d(TAG, "Audit logged: $actionType by $userName (ID: $userId) - Success: $wasSuccessful")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging audit", e)
            }
        }
    }
    
    /**
     * Log successful login
     */
    fun logLogin(userId: Long, userName: String, roleLevel: Int) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_LOGIN,
            userId = userId,
            userName = userName,
            roleLevel = roleLevel,
            wasSuccessful = true
        )
    }
    
    /**
     * Log failed login attempt
     */
    fun logFailedLogin(userName: String, reason: String) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_LOGIN,
            userId = -1,
            userName = userName,
            roleLevel = -1,
            wasSuccessful = false,
            failureReason = reason
        )
    }
    
    /**
     * Log logout
     */
    fun logLogout(userId: Long, userName: String, roleLevel: Int) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_LOGOUT,
            userId = userId,
            userName = userName,
            roleLevel = roleLevel
        )
    }
    
    /**
     * Log void action (requires manager override)
     */
    fun logVoid(
        managerId: Long,
        managerName: String,
        managerRole: Int,
        targetType: String,
        targetId: String,
        reason: String
    ) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_VOID_ITEM,
            userId = managerId,
            userName = managerName,
            roleLevel = managerRole,
            targetType = targetType,
            targetId = targetId,
            actionDetails = reason
        )
    }
    
    /**
     * Log refund action (requires manager override)
     */
    fun logRefund(
        managerId: Long,
        managerName: String,
        managerRole: Int,
        orderId: String,
        amount: Double,
        reason: String
    ) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_REFUND,
            userId = managerId,
            userName = managerName,
            roleLevel = managerRole,
            targetType = SecurityAuditEntity.TARGET_ORDER,
            targetId = orderId,
            actionDetails = "Amount: $amount, Reason: $reason"
        )
    }
    
    /**
     * Log price override
     */
    fun logPriceOverride(
        managerId: Long,
        managerName: String,
        managerRole: Int,
        itemId: String,
        originalPrice: Double,
        newPrice: Double,
        reason: String
    ) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_PRICE_OVERRIDE,
            userId = managerId,
            userName = managerName,
            roleLevel = managerRole,
            targetType = SecurityAuditEntity.TARGET_MENU_ITEM,
            targetId = itemId,
            actionDetails = "Original: $originalPrice, New: $newPrice, Reason: $reason"
        )
    }
    
    /**
     * Log manager override
     */
    fun logManagerOverride(
        managerId: Long,
        managerName: String,
        managerRole: Int,
        action: String,
        details: String
    ) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_MANAGER_OVERRIDE,
            userId = managerId,
            userName = managerName,
            roleLevel = managerRole,
            actionDetails = "$action: $details"
        )
    }
    
    /**
     * Log access denied
     */
    fun logAccessDenied(
        userId: Long,
        userName: String,
        roleLevel: Int,
        attemptedAction: String,
        reason: String
    ) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_ACCESS_DENIED,
            userId = userId,
            userName = userName,
            roleLevel = roleLevel,
            actionDetails = attemptedAction,
            wasSuccessful = false,
            failureReason = reason
        )
    }
    
    /**
     * Log session timeout
     */
    fun logSessionTimeout(userId: Long, userName: String, roleLevel: Int) {
        logAction(
            actionType = SecurityAuditEntity.ACTION_SESSION_TIMEOUT,
            userId = userId,
            userName = userName,
            roleLevel = roleLevel
        )
    }
    
    /**
     * Log chief performance metrics
     */
    fun logChiefPerformance(
        chiefId: Long,
        chiefName: String,
        detailId: Long,
        itemName: String,
        prepTimeSeconds: Long
    ) {
        logAction(
            actionType = "CHIEF_PERFORMANCE",
            userId = chiefId,
            userName = chiefName,
            roleLevel = 2, // Chief role level
            targetType = "ORDER_DETAIL",
            targetId = detailId.toString(),
            actionDetails = "Item: $itemName, Prep Time: ${prepTimeSeconds}s"
        )
    }
    
    companion object {
        private const val TAG = "AuditLogger"
    }
}

/**
 * Provides device information for audit logging
 */
@Singleton
class DeviceInfoProvider @Inject constructor() {
    
    // We renamed the internal variable to _deviceId to avoid the name clash
    private val _deviceId: String by lazy {
        java.util.UUID.randomUUID().toString()
    }
    
    fun getDeviceId(): String = _deviceId
    
    fun getIpAddress(): String? {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces()
                .toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.address.size == 4 }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
