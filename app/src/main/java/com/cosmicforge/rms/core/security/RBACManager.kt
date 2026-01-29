package com.cosmicforge.rms.core.security

import com.cosmicforge.rms.data.database.entities.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Role-Based Access Control Manager
 * Defines permissions for each role level and enforces access control
 */
@Singleton
class RBACManager @Inject constructor() {
    
    /**
     * Check if user has permission for an action
     */
    fun hasPermission(userRole: Int, permission: Permission): Boolean {
        val rolePermissions = getRolePermissions(userRole)
        return permission in rolePermissions
    }
    
    /**
     * Check if user can perform manager override
     * Only Owner (1) and Manager (2) can override
     */
    fun canPerformManagerOverride(userRole: Int): Boolean {
        return userRole == UserEntity.ROLE_OWNER || userRole == UserEntity.ROLE_MANAGER
    }
    
    /**
     * Check if user can void items
     */
    fun canVoidItem(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.VOID_ITEM)
    }
    
    /**
     * Check if user can process refunds
     */
    fun canProcessRefund(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.PROCESS_REFUND)
    }
    
    /**
     * Check if user can override prices
     */
    fun canOverridePrice(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.OVERRIDE_PRICE)
    }
    
    /**
     * Check if user can access admin dashboard
     */
    fun canAccessDashboard(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.VIEW_DASHBOARD)
    }
    
    /**
     * Check if user can manage other users
     */
    fun canManageUsers(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.MANAGE_USERS)
    }
    
    /**
     * Check if user can modify menu
     */
    fun canManageMenu(userRole: Int): Boolean {
        return hasPermission(userRole, Permission.MANAGE_MENU)
    }
    
    /**
     * Check if user is restricted to specific station (KDS)
     */
    fun isStationLocked(userRole: Int, stationId: String?): Boolean {
        // Chiefs are locked to their assigned station
        if (userRole == UserEntity.ROLE_CHIEF && stationId != null) {
            return true
        }
        return false
    }
    
    /**
     * Get allowed views for role
     */
    fun getAllowedViews(userRole: Int, stationId: String?): List<AllowedView> {
        return when (userRole) {
            UserEntity.ROLE_OWNER -> listOf(
                AllowedView.DASHBOARD,
                AllowedView.FLOOR_MAP,
                AllowedView.ORDER_ENTRY,
                AllowedView.KDS,
                AllowedView.USERS,
                AllowedView.MENU,
                AllowedView.REPORTS
            )
            UserEntity.ROLE_MANAGER -> listOf(
                AllowedView.DASHBOARD,
                AllowedView.FLOOR_MAP,
                AllowedView.ORDER_ENTRY,
                AllowedView.KDS,
                AllowedView.REPORTS
            )
            UserEntity.ROLE_WAITER -> listOf(
                AllowedView.FLOOR_MAP,
                AllowedView.ORDER_ENTRY
            )
            UserEntity.ROLE_CHIEF -> listOf(
                AllowedView.KDS // Station locked
            )
            else -> emptyList()
        }
    }
    
    private fun getRolePermissions(roleLevel: Int): Set<Permission> {
        return when (roleLevel) {
            UserEntity.ROLE_OWNER -> setOf(
                Permission.VIEW_DASHBOARD,
                Permission.MANAGE_USERS,
                Permission.MANAGE_MENU,
                Permission.MANAGE_TABLES,
                Permission.CREATE_ORDER,
                Permission.EDIT_ORDER,
                Permission.VOID_ITEM,
                Permission.VOID_ORDER,
                Permission.PROCESS_REFUND,
                Permission.OVERRIDE_PRICE,
                Permission.OVERRIDE_DISCOUNT,
                Permission.VIEW_REPORTS,
                Permission.VIEW_KDS,
                Permission.VIEW_AUDIT_LOGS,
                Permission.MANAGE_MODULES
            )
            
            UserEntity.ROLE_MANAGER -> setOf(
                Permission.VIEW_DASHBOARD,
                Permission.MANAGE_TABLES,
                Permission.CREATE_ORDER,
                Permission.EDIT_ORDER,
                Permission.VOID_ITEM,
                Permission.VOID_ORDER,
                Permission.PROCESS_REFUND,
                Permission.OVERRIDE_PRICE,
                Permission.OVERRIDE_DISCOUNT,
                Permission.VIEW_REPORTS,
                Permission.VIEW_KDS
            )
            
            UserEntity.ROLE_WAITER -> setOf(
                Permission.CREATE_ORDER,
                Permission.EDIT_ORDER,
                Permission.MANAGE_TABLES
            )
            
            UserEntity.ROLE_CHIEF -> setOf(
                Permission.VIEW_KDS,
                Permission.CLAIM_ORDER_DETAIL,
                Permission.MARK_READY
            )
            
            else -> emptySet()
        }
    }
}

/**
 * Application permissions
 */
enum class Permission {
    // Dashboard & Admin
    VIEW_DASHBOARD,
    MANAGE_USERS,
    MANAGE_MODULES,
    VIEW_AUDIT_LOGS,
    VIEW_REPORTS,
    
    // Menu Management
    MANAGE_MENU,
    
    // Table Management
    MANAGE_TABLES,
    
    // Order Operations
    CREATE_ORDER,
    EDIT_ORDER,
    VOID_ITEM,
    VOID_ORDER,
    
    // Financial
    PROCESS_REFUND,
    OVERRIDE_PRICE,
    OVERRIDE_DISCOUNT,
    
    // Kitchen
    VIEW_KDS,
    CLAIM_ORDER_DETAIL,
    MARK_READY
}

/**
 * Allowed application views
 */
enum class AllowedView {
    DASHBOARD,
    FLOOR_MAP,
    ORDER_ENTRY,
    KDS,
    USERS,
    MENU,
    REPORTS
}
