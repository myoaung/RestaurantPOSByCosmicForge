package com.cosmicforge.pos.core.security

import android.util.Log
import com.cosmicforge.pos.data.database.dao.UserDao
import com.cosmicforge.pos.data.database.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication manager with PIN-based authentication
 * Handles login, logout, and session management
 */
@Singleton
class AuthenticationManager @Inject constructor(
    private val userDao: UserDao,
    private val auditLogger: AuditLogger,
    private val rbacManager: RBACManager
) {
    
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()
    
    private val _sessionState = MutableStateFlow(SessionState.LOGGED_OUT)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private var sessionStartTime: Long = 0
    private var lastActivityTime: Long = 0
    
    /**
     * Authenticate user with userName and PIN
     */
    suspend fun authenticateWithPin(userName: String, pin: String): AuthResult {
        Log.d(TAG, "Authenticating user: $userName")
        
        val user = userDao.getUserByName(userName)
        
        if (user == null) {
            Log.w(TAG, "User not found: $userName")
            auditLogger.logFailedLogin(userName, "User not found")
            return AuthResult.Failure("User not found")
        }
        
        if (!user.isActive) {
            Log.w(TAG, "User is inactive: $userName")
            auditLogger.logFailedLogin(userName, "User is inactive")
            return AuthResult.Failure("User is inactive")
        }
        
        // Verify PIN
        val hashedPin = hashPin(pin)
        if (hashedPin != user.pinHash) {
            Log.w(TAG, "Invalid PIN for user: $userName")
            auditLogger.logFailedLogin(userName, "Invalid PIN")
            return AuthResult.Failure("Invalid PIN")
        }
        
        // Authentication successful
        _currentUser.value = user
        _sessionState.value = SessionState.ACTIVE
        sessionStartTime = System.currentTimeMillis()
        lastActivityTime = System.currentTimeMillis()
        
        auditLogger.logLogin(user.userId, user.userName, user.roleLevel)
        Log.d(TAG, "User authenticated: ${user.userName} (${user.getRoleName()})")
        
        return AuthResult.Success(user)
    }
    
    /**
     * Verify PIN for manager override
     * Returns the manager user if PIN is valid and they have override permission
     */
    suspend fun verifyManagerOverride(userName: String, pin: String): ManagerOverrideResult {
        Log.d(TAG, "Verifying manager override for: $userName")
        
        val user = userDao.getUserByName(userName)
        
        if (user == null || !user.isActive) {
            return ManagerOverrideResult.Failure("Invalid credentials")
        }
        
        // Verify PIN
        val hashedPin = hashPin(pin)
        if (hashedPin != user.pinHash) {
            auditLogger.logFailedLogin(userName, "Invalid PIN for override")
            return ManagerOverrideResult.Failure("Invalid PIN")
        }
        
        // Check if user has manager override permission
        if (!rbacManager.canPerformManagerOverride(user.roleLevel)) {
            auditLogger.logAccessDenied(
                user.userId,
                user.userName,
                user.roleLevel,
                "Manager Override",
                "Insufficient permissions"
            )
            return ManagerOverrideResult.Failure("Only Owner or Manager can override")
        }
        
        Log.d(TAG, "Manager override verified: ${user.userName}")
        return ManagerOverrideResult.Success(user)
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        _currentUser.value?.let { user ->
            auditLogger.logLogout(user.userId, user.userName, user.roleLevel)
            Log.d(TAG, "User logged out: ${user.userName}")
        }
        
        _currentUser.value = null
        _sessionState.value = SessionState.LOGGED_OUT
        sessionStartTime = 0
        lastActivityTime = 0
    }
    
    /**
     * Update last activity time
     */
    fun updateActivity() {
        if (_sessionState.value == SessionState.ACTIVE) {
            lastActivityTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Check for session timeout
     */
    fun checkSessionTimeout(): Boolean {
        if (_sessionState.value != SessionState.ACTIVE) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val inactiveTime = currentTime - lastActivityTime
        
        if (inactiveTime > SESSION_TIMEOUT_MS) {
            _currentUser.value?.let { user ->
                auditLogger.logSessionTimeout(user.userId, user.userName, user.roleLevel)
                Log.d(TAG, "Session timeout for user: ${user.userName}")
            }
            
            _sessionState.value = SessionState.TIMED_OUT
            return true
        }
        
        return false
    }
    
    /**
     * Get current session duration
     */
    fun getSessionDuration(): Long {
        if (sessionStartTime == 0L) return 0L
        return System.currentTimeMillis() - sessionStartTime
    }
    
    /**
     * Get time until session timeout
     */
    fun getTimeUntilTimeout(): Long {
        if (_sessionState.value != SessionState.ACTIVE) return 0L
        val elapsed = System.currentTimeMillis() - lastActivityTime
        return (SESSION_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
    }
    
    /**
     * Check if current user has permission
     */
    fun hasPermission(permission: Permission): Boolean {
        val user = _currentUser.value ?: return false
        return rbacManager.hasPermission(user.roleLevel, permission)
    }
    
    /**
     * Get allowed views for current user
     */
    fun getAllowedViews(): List<AllowedView> {
        val user = _currentUser.value ?: return emptyList()
        return rbacManager.getAllowedViews(user.roleLevel, user.stationId)
    }
    
    /**
     * Check if current user is station locked (KDS only)
     */
    fun isStationLocked(): Boolean {
        val user = _currentUser.value ?: return false
        return rbacManager.isStationLocked(user.roleLevel, user.stationId)
    }
    
    /**
     * Hash PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        private const val TAG = "AuthenticationManager"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
}

/**
 * Authentication result
 */
sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Failure(val reason: String) : AuthResult()
}

/**
 * Manager override result
 */
sealed class ManagerOverrideResult {
    data class Success(val manager: UserEntity) : ManagerOverrideResult()
    data class Failure(val reason: String) : ManagerOverrideResult()
}

/**
 * Session state
 */
enum class SessionState {
    LOGGED_OUT,
    ACTIVE,
    TIMED_OUT
}
