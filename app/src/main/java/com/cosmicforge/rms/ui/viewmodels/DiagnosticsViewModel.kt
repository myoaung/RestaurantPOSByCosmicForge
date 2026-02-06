package com.cosmicforge.rms.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.repository.DiagnosticsRepository
import com.cosmicforge.rms.data.repository.SubscriptionTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for system diagnostics
 * Auto-refreshes every 30 seconds
 * v10.3.1: Diagnostics & Monitoring
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) : ViewModel() {
    
    private val _metrics = MutableStateFlow(DiagnosticsMetrics())
    val metrics: StateFlow<DiagnosticsMetrics> = _metrics.asStateFlow()
    
    init {
        startAutoRefresh()
    }
    
    /**
     * Start auto-refresh loop (every 30 seconds)
     */
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                refreshMetrics()
                delay(30_000) // 30 seconds
            }
        }
    }
    
    /**
     * Manually refresh metrics
     */
    fun refreshMetrics() {
        viewModelScope.launch {
            val userCount = diagnosticsRepository.getUserCount()
            val activeUserCount = diagnosticsRepository.getActiveUserCount()
            val orderCount = diagnosticsRepository.getOrderCount()
            val pendingSyncCount = diagnosticsRepository.getPendingSyncCount()
            val deviceCount = diagnosticsRepository.getActiveDeviceCount()
            val tier = diagnosticsRepository.getSubscriptionTier()
            val atCapacity = diagnosticsRepository.isAtCapacity()
            
            _metrics.value = DiagnosticsMetrics(
                totalUsers = userCount,
                activeUsers = activeUserCount,
                totalOrders = orderCount,
                pendingSync = pendingSyncCount,
                activeDevices = deviceCount,
                subscriptionTier = tier,
                isAtCapacity = atCapacity
            )
        }
    }
}

/**
 * Diagnostics metrics data class
 */
data class DiagnosticsMetrics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalOrders: Int = 0,
    val pendingSync: Int = 0,
    val activeDevices: Int = 0,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.SILVER,
    val isAtCapacity: Boolean = false
)
