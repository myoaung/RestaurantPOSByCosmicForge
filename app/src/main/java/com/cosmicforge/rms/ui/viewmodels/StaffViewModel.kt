package com.cosmicforge.rms.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Staff Management
 * v9.7: Staff Management Foundation
 */
@HiltViewModel
class StaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {
    
    /**
     * Get all staff (RBAC enforced)
     */
    fun getAllStaff(currentUser: UserEntity): Flow<List<UserEntity>> {
        return staffRepository.getAllStaff(currentUser) ?: flowOf(emptyList())
    }
    
    /**
     * Create new staff member
     */
    fun createStaff(
        userName: String,
        pin: String,
        roleLevel: Int,
        nrcNumber: String?,
        currentUser: UserEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = staffRepository.createStaff(
                userName = userName,
                pin = pin,
                roleLevel = roleLevel,
                nrcNumber = nrcNumber,
                joinDate = System.currentTimeMillis(),
                currentUser = currentUser
            )
            
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> onError(error.message ?: "Unknown error") }
            )
        }
    }
    
    /**
     * Deactivate staff (Owner only)
     */
    fun deactivateStaff(
        userId: Long,
        currentUser: UserEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = staffRepository.deactivateStaff(userId, currentUser)
            if (success) {
                onSuccess()
            } else {
                onError("Access denied: only Owner can deactivate staff")
            }
        }
    }
}
