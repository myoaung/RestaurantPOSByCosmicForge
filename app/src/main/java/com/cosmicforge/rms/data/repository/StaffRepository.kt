package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.data.database.dao.BlacklistDao
import com.cosmicforge.rms.data.database.dao.UserDao
import com.cosmicforge.rms.data.database.entities.BlacklistEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.utils.StaffUtils
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for staff management with NRC blacklist protection
 * v9.7 + v10.5: Staff Management + Security
 */
@Singleton
class StaffRepository @Inject constructor(
    private val userDao: UserDao,
    private val blacklistDao: BlacklistDao
) {
    
    /**
     * Get all active staff (Manager/Owner only)
     */
    fun getAllStaff(currentUser: UserEntity): Flow<List<UserEntity>>? {
        // RBAC: Only Manager and Owner can view all staff
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null // Waiters/Chiefs cannot see staff list
        }
        return userDao.getAllActiveUsers()
    }
    
    /**
     * Create new staff member with blacklist check
     */
    suspend fun createStaff(
        userName: String,
        pin: String,
        roleLevel: Int,
        nrcNumber: String?,
        joinDate: Long,
        currentUser: UserEntity
    ): Result<Long> {
        // RBAC: Only Manager and Owner can create staff
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return Result.failure(Exception("Access denied: insufficient permissions"))
        }
        
        // NRC blacklist check
        if (!nrcNumber.isNullOrBlank()) {
            // Validate NRC format
            if (!StaffUtils.validateNRC(nrcNumber)) {
                return Result.failure(Exception("Invalid NRC format"))
            }
            
            // Check blacklist
            val blacklisted = blacklistDao.checkNRC(nrcNumber)
            if (blacklisted != null) {
                return Result.failure(
                    Exception("SECURITY ALERT: This NRC is blacklisted. Reason: ${blacklisted.reasonForBlacklist}")
                )
            }
        }
        
        // Create user
        val user = UserEntity(
            userName = userName,
            pinHash = hashPin(pin),
            roleLevel = roleLevel,
            nrcNumber = nrcNumber,
            joinDate = joinDate,
            isActive = true
        )
        
        val userId = userDao.insertUser(user)
        return Result.success(userId)
    }
    
    /**
     * Update staff information (Manager/Owner only)
     */
    suspend fun updateStaff(user: UserEntity, currentUser: UserEntity): Boolean {
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return false
        }
        
        userDao.updateUser(user)
        return true
    }
    
    /**
     * Deactivate staff (offboarding) - Owner only
     */
    suspend fun deactivateStaff(userId: Long, currentUser: UserEntity): Boolean {
        // RBAC: Only Owner can deactivate
        if (currentUser.roleLevel != UserEntity.ROLE_OWNER) {
            return false
        }
        
        userDao.deactivateUser(userId)
        return true
    }
    
    /**
     * Add to blacklist (Owner only)
     */
    suspend fun addToBlacklist(
        nrcNumber: String,
        staffName: String,
        reason: String,
        currentUser: UserEntity
    ): Boolean {
        // RBAC: Only Owner can blacklist
        if (currentUser.roleLevel != UserEntity.ROLE_OWNER) {
            return false
        }
        
        val blacklist = BlacklistEntity(
            nrcNumber = nrcNumber,
            staffName = staffName,
            reasonForBlacklist = reason,
            blacklistedBy = currentUser.userId
        )
        
        blacklistDao.insertBlacklist(blacklist)
        return true
    }
    
    /**
     * Get all blacklisted NRCs (Manager/Owner only)
     */
    fun getBlacklist(currentUser: UserEntity): Flow<List<BlacklistEntity>>? {
        if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
            return null // Staff shielded
        }
        return blacklistDao.getAllBlacklisted()
    }
    
    /**
     * Hash PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
