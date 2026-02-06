package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.data.database.dao.LeaveRequestDao
import com.cosmicforge.rms.data.database.entities.LeaveRequestEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.utils.StaffUtils
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for leave management with RBAC enforcement
 * v10.6: Leave Management System
 */
@Singleton
class LeaveRepository @Inject constructor(
    private val leaveRequestDao: LeaveRequestDao
) {
    
    /**
     * Get all leave requests (Manager/Owner only)
     */
    fun getAllRequests(currentUser: UserEntity): Flow<List<LeaveRequestEntity>>? {
        // RBAC: Only Manager (2) and Owner (1) can view all requests
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null // Access denied
        }
        return leaveRequestDao.getAllRequests()
    }
    
    /**
     * Get pending leave requests for approval (Manager/Owner only)
     */
    fun getPendingRequests(currentUser: UserEntity): Flow<List<LeaveRequestEntity>>? {
        // RBAC: Only Manager and Owner can approve
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null
        }
        return leaveRequestDao.getPendingRequests()
    }
    
    /**
     * Get user's own leave requests (All roles)
     */
    fun getMyRequests(userId: Long): Flow<List<LeaveRequestEntity>> {
        return leaveRequestDao.getRequestsByUser(userId)
    }
    
    /**
     * Submit leave request (All roles)
     */
    suspend fun submitRequest(
        userId: Long,
        leaveType: String,
        startDate: Long,
        endDate: Long,
        reason: String
    ): Long {
        val request = LeaveRequestEntity(
            userId = userId,
            leaveType = leaveType,
            startDate = startDate,
            endDate = endDate,
            reason = reason
        )
        return leaveRequestDao.insertRequest(request)
    }
    
    /**
     * Approve or reject leave request (Manager/Owner only)
     */
    suspend fun reviewRequest(
        requestId: Long,
        approved: Boolean,
        reviewerId: Long,
        currentUser: UserEntity
    ): Boolean {
        // RBAC: Only Manager and Owner can approve
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return false // Access denied
        }
        
        val status = if (approved) 
            LeaveRequestEntity.STATUS_APPROVED 
        else 
            LeaveRequestEntity.STATUS_REJECTED
        
        leaveRequestDao.approveOrRejectRequest(
            requestId = requestId,
            status = status,
            reviewedBy = reviewerId,
            reviewedAt = System.currentTimeMillis()
        )
        
        return true
    }
    
    /**
     * Calculate leave balance for user
     * Entitlement: 10 base days + 2 days per year of service
     */
    suspend fun calculateLeaveBalance(user: UserEntity): Int {
        val tenure = StaffUtils.calculateTenure(user.joinDate)
        val yearsOfService = tenure.first
        
        // Myanmar labor law: typically 10-15 days annual leave
        val entitledDays = 10 + (yearsOfService * 2)
        
        // Get approved leave days
        val approvedRequests = leaveRequestDao.getRequestsByUser(user.userId)
            .toString() // TODO: Convert Flow to list
        
        // For now, return entitled days (will be enhanced)
        return entitledDays
    }
    
    /**
     * Calculate days between two timestamps
     */
    private fun calculateDays(startDate: Long, endDate: Long): Int {
        return TimeUnit.MILLISECONDS.toDays(endDate - startDate).toInt() + 1
    }
}
