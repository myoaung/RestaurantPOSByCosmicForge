package com.cosmicforge.pos.data.repository

import com.cosmicforge.pos.core.security.*
import com.cosmicforge.pos.data.database.dao.UserDao
import com.cosmicforge.pos.data.database.entities.SecurityAuditEntity
import com.cosmicforge.pos.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication and user management
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val authenticationManager: AuthenticationManager,
    private val auditLogger: AuditLogger
) {
    
    /**
     * Login with PIN
     */
    suspend fun login(userName: String, pin: String): AuthResult {
        return authenticationManager.authenticateWithPin(userName, pin)
    }
    
    /**
     * Verify manager override
     */
    suspend fun verifyManagerOverride(userName: String, pin: String): ManagerOverrideResult {
        return authenticationManager.verifyManagerOverride(userName, pin)
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        authenticationManager.logout()
    }
    
    /**
     * Get current user
     */
    fun getCurrentUser(): Flow<UserEntity?> {
        return authenticationManager.currentUser
    }
    
    /**
     * Get active users
     */
    fun getAllActiveUsers(): Flow<List<UserEntity>> {
        return userDao.getAllActiveUsers()
    }
    
    /**
     * Get users by role
     */
    fun getUsersByRole(roleLevel: Int): Flow<List<UserEntity>> {
        return userDao.getUsersByRole(roleLevel)
    }
    
    /**
     * Create new user
     */
    suspend fun createUser(user: UserEntity): Long {
        val currentUser = authenticationManager.currentUser.value
        currentUser?.let {
            auditLogger.logAction(
                actionType = SecurityAuditEntity.ACTION_MANAGER_OVERRIDE,
                userId = it.userId,
                userName = it.userName,
                roleLevel = it.roleLevel,
                targetType = SecurityAuditEntity.TARGET_USER,
                targetId = user.userName,
                actionDetails = "Created new user: ${user.userName} with role ${user.getRoleName()}"
            )
        }
        
        return userDao.insertUser(user)
    }
    
    /**
     * Update user
     */
    suspend fun updateUser(user: UserEntity) {
        val currentUser = authenticationManager.currentUser.value
        currentUser?.let {
            auditLogger.logAction(
                actionType = SecurityAuditEntity.ACTION_MANAGER_OVERRIDE,
                userId = it.userId,
                userName = it.userName,
                roleLevel = it.roleLevel,
                targetType = SecurityAuditEntity.TARGET_USER,
                targetId = user.userName,
                actionDetails = "Updated user: ${user.userName}"
            )
        }
        
        userDao.updateUser(user)
    }
    
    /**
     * Deactivate user
     */
    suspend fun deactivateUser(userId: Long) {
        val currentUser = authenticationManager.currentUser.value
        currentUser?.let {
            auditLogger.logAction(
                actionType = SecurityAuditEntity.ACTION_MANAGER_OVERRIDE,
                userId = it.userId,
                userName = it.userName,
                roleLevel = it.roleLevel,
                targetType = SecurityAuditEntity.TARGET_USER,
                targetId = userId.toString(),
                actionDetails = "Deactivated user ID: $userId"
            )
        }
        
        userDao.deactivateUser(userId)
    }
}
