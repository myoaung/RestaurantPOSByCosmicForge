package com.cosmicforge.rms.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.database.entities.RewardEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.data.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Reward History
 * v10.0: Gamification Engine
 */
@HiltViewModel
class RewardViewModel @Inject constructor(
    private val rewardRepository: RewardRepository
) : ViewModel() {
    
    /**
     * Get all rewards (RBAC enforced)
     */
    fun getAllRewards(currentUser: UserEntity): Flow<List<RewardEntity>> {
        return rewardRepository.getAllRewards(currentUser) ?: flowOf(emptyList())
    }
    
    /**
     * Get rewards for specific month
     */
    fun getRewardsByMonth(monthYear: String, currentUser: UserEntity): Flow<List<RewardEntity>> {
        return rewardRepository.getRewardsByMonth(monthYear, currentUser) ?: flowOf(emptyList())
    }
    
    /**
     * Get user's own rewards
     */
    fun getMyRewards(userId: Long): Flow<List<RewardEntity>> {
        return rewardRepository.getMyRewards(userId)
    }
    
    /**
     * Mark reward as paid (Owner only)
     */
    fun markAsPaid(
        rewardId: Long,
        currentUser: UserEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = rewardRepository.markAsPaid(rewardId, currentUser)
            if (success) {
                onSuccess()
            } else {
                onError("Access denied: only Owner can mark rewards as paid")
            }
        }
    }
}
