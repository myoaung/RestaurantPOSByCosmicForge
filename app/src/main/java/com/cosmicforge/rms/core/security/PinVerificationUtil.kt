package com.cosmicforge.rms.core.security

import com.cosmicforge.rms.data.database.dao.UserDao
import com.cosmicforge.rms.data.database.entities.UserEntity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PIN Verification Utility
 * 
 * Security Gate Requirement: "Access to DeadLetterVault must remain restricted 
 * until the ManagerAuth event is captured"
 * 
 * Verifies 6-digit PINs against UserEntity with role-based access control
 */
@Singleton
class PinVerificationUtil @Inject constructor(
    private val userDao: UserDao
) {
    
    /**
     * Verify PIN for manager-level access
     * 
     * @param pin 6-digit PIN entered by user
     * @return UserEntity if PIN is valid and user has manager/owner role, null otherwise
     */
    suspend fun verifyManagerPin(pin: String): UserEntity? {
        // Validate PIN format
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return null
        }
        
        // Hash the entered PIN
        val pinHash = hashPin(pin)
        
        // Get all active users with manager or owner role
        // Note: getUsersByRole returns Flow, we need to collect it
        val ownerFlow = userDao.getUsersByRole(UserEntity.ROLE_OWNER)
        val managerFlow = userDao.getUsersByRole(UserEntity.ROLE_MANAGER)
        
        // Collect both flows and combine
        var matchedUser: UserEntity? = null
        
        // Check owners first
        ownerFlow.collect { owners ->
            matchedUser = owners.firstOrNull { user -> user.pinHash == pinHash }
        }
        
        // If not found in owners, check managers
        if (matchedUser == null) {
            managerFlow.collect { managers ->
                matchedUser = managers.firstOrNull { user -> user.pinHash == pinHash }
            }
        }
        
        return matchedUser
    }
    
    /**
     * Verify PIN for any user (for login)
     * 
     * @param userName User name to verify
     * @param pin PIN entered by user
     * @return UserEntity if credentials are valid, null otherwise
     */
    suspend fun verifyUserPin(userName: String, pin: String): UserEntity? {
        // Validate PIN format
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return null
        }
        
        // Get user by name
        val user = userDao.getUserByName(userName) ?: return null
        
        // Verify PIN hash
        val pinHash = hashPin(pin)
        return if (user.pinHash == pinHash && user.isActive) {
            user
        } else {
            null
        }
    }
    
    /**
     * Hash PIN using SHA-256
     * 
     * @param pin Plain text PIN
     * @return SHA-256 hash as hex string
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if user has manager-level access
     * 
     * @param user UserEntity to check
     * @return true if user is owner or manager
     */
    fun hasManagerAccess(user: UserEntity): Boolean {
        return user.roleLevel == UserEntity.ROLE_OWNER || 
               user.roleLevel == UserEntity.ROLE_MANAGER
    }
    
    /**
     * Generate random 6-digit PIN for new users
     * 
     * @return Random 6-digit PIN as string
     */
    fun generateRandomPin(): String {
        return (100000..999999).random().toString()
    }
}
