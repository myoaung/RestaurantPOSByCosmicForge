package com.cosmicforge.rms.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.database.entities.LeaveRequestEntity
import com.cosmicforge.rms.data.repository.LeaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Leave Management
 * v10.5: Leave Request & Approval
 */
@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository
) : ViewModel() {
    
    /**
     * Get user's own leave requests
     */
    fun getMyLeaveRequests(userId: Long): Flow<List<LeaveRequestEntity>> {
        return leaveRepository.getMyLeaveRequests(userId)
    }
    
    /**
     * Submit leave request
     */
    fun submitLeaveRequest(
        userId: Long,
        leaveType: String,
        startDate: Long,
        endDate: Long,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = leaveRepository.submitLeaveRequest(
                userId = userId,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                reason = reason
            )
            
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> onError(error.message ?: "Failed to submit request") }
            )
        }
    }
    
    /**
     * Approve leave request (Manager/Owner only)
     */
    fun approveLeave(
        requestId: Long,
        currentUser: com.cosmicforge.rms.data.database.entities.UserEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = leaveRepository.approveLeave(requestId, currentUser)
            if (success) {
                onSuccess()
            } else {
                onError("Access denied: Manager/Owner only")
            }
        }
    }
    
    /**
     * Reject leave request (Manager/Owner only)
     */
    fun rejectLeave(
        requestId: Long,
        currentUser: com.cosmicforge.rms.data.database.entities.UserEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = leaveRepository.rejectLeave(requestId, currentUser)
            if (success) {
                onSuccess()
            } else {
                onError("Access denied: Manager/Owner only")
            }
        }
    }
}
