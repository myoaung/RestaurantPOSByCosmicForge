```kotlin
package com.cosmicforge.rms.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.core.security.AuthResult
import com.cosmicforge.rms.core.security.ManagerOverrideResult
import com.cosmicforge.rms.data.database.dao.UserDao
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.data.repository.AuthRepository
import com.cosmicforge.rms.data.repository.RewardRepository
import com.cosmicforge.rms.data.repository.SettingsRepository
import com.cosmicforge.rms.utils.PerformanceCalculator
import com.cosmicforge.rms.utils.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ViewModel for login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository, // Keep authRepository for login/manager override
    private val userDao: UserDao,
    private val rewardRepository: RewardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    private val _selectedUser = MutableStateFlow<UserEntity?>(null)
    val selectedUser: StateFlow<UserEntity?> = _selectedUser.asStateFlow()
    
    private val _activeUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val activeUsers: StateFlow<List<UserEntity>> = _activeUsers.asStateFlow()
    
    private val _currentMonthWinnerId = MutableStateFlow<Long?>(null)
    val currentMonthWinnerId: StateFlow<Long?> = _currentMonthWinnerId.asStateFlow()
    
    init {
        loadActiveUsers()
        loadCurrentMonthWinner()
    }
    
    private fun loadActiveUsers() {
        viewModelScope.launch {
            userDao.getAllActiveUsers().collect { users ->
                _activeUsers.value = users
                _uiState.value = LoginUiState.UserSelection
            }
        }
    }
    
    private fun loadCurrentMonthWinner() {
        viewModelScope.launch {
            // Only load winner if gamification is enabled
            settingsRepository.isGamificationEnabled.collect { isEnabled ->
                if (isEnabled) {
                    val topPerformer = rewardRepository.getTopPerformerForMonth(
                        PerformanceCalculator.getCurrentMonthYear()
                    )
                    _currentMonthWinnerId.value = topPerformer?.userId
                } else {
                    _currentMonthWinnerId.value = null
                }
            }
        }
    }
    
    /**
     * Select user for login
     */
    fun selectUser(user: UserEntity) {
        _selectedUser.value = user
        _uiState.value = LoginUiState.EnterPin(user)
    }
    
    /**
     * Go back to user selection
     */
    fun backToUserSelection() {
        _selectedUser.value = null
        _uiState.value = LoginUiState.UserSelection
    }
    
    /**
     * Login with PIN
     */
    fun login(pin: String) {
        val user = _selectedUser.value ?: return
        
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            
            when (val result = authRepository.login(user.userName, pin)) {
                is AuthResult.Success -> {
                    _uiState.value = LoginUiState.Success(result.user)
                }
                is AuthResult.Failure -> {
                    _uiState.value = LoginUiState.Error(result.reason)
                    // Reset to PIN entry after delay
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = LoginUiState.EnterPin(user)
                }
            }
        }
    }
    
    /**
     * Verify manager override PIN
     */
    fun verifyManagerOverride(userName: String, pin: String, onResult: (ManagerOverrideResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.verifyManagerOverride(userName, pin)
            onResult(result)
        }
    }
}

/**
 * Login UI state
 */
sealed class LoginUiState {
    object SelectUser : LoginUiState()
    data class EnterPin(val user: UserEntity) : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: UserEntity) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
