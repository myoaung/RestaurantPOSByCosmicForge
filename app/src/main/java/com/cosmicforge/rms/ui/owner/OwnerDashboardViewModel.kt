package com.cosmicforge.rms.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.data.repository.AnalyticsRepository
import com.cosmicforge.rms.data.repository.ChiefPerformance
import com.cosmicforge.rms.data.repository.ParcelAdjustment
import com.cosmicforge.rms.data.repository.SalesSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for owner dashboard
 */
@HiltViewModel
class OwnerDashboardViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _salesSummary = MutableStateFlow<SalesSummary?>(null)
    val salesSummary: StateFlow<SalesSummary?> = _salesSummary.asStateFlow()
    
    private val _parcelAdjustments = MutableStateFlow<List<ParcelAdjustment>>(emptyList())
    val parcelAdjustments: StateFlow<List<ParcelAdjustment>> = _parcelAdjustments.asStateFlow()
    
    private val _chiefPerformance = MutableStateFlow<List<ChiefPerformance>>(emptyList())
    val chiefPerformance: StateFlow<List<ChiefPerformance>> = _chiefPerformance.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Load all dashboard data
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load sales summary
                _salesSummary.value = analyticsRepository.getTodaySalesSummary()
                
                // Load parcel adjustments
                _parcelAdjustments.value = analyticsRepository.getTodayParcelAdjustments()
                
                // Load chief performance
                _chiefPerformance.value = analyticsRepository.getTodayChiefPerformance()
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Generate end-of-day report
     */
    fun generateEndOfDayReport(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val report = analyticsRepository.generateEndOfDayReport()
                
                // Format report as text
                val reportText = buildString {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("   COSMIC FORGE POS")
                    appendLine("   END OF DAY REPORT")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine()
                    appendLine("Date: ${report.date}")
                    appendLine()
                    appendLine("═══ SALES SUMMARY ═══")
                    appendLine("Total Sales: ${report.salesSummary.totalSales.toInt()} MMK")
                    appendLine("  • Cash: ${report.salesSummary.cashSales.toInt()} MMK")
                    appendLine("  • KPay: ${report.salesSummary.kpaySales.toInt()} MMK")
                    appendLine("  • CB Pay: ${report.salesSummary.cbpaySales.toInt()} MMK")
                    appendLine()
                    appendLine("Total Orders: ${report.salesSummary.totalOrders}")
                    appendLine("  • Dine-In: ${report.dineInCount}")
                    appendLine("  • Parcel: ${report.parcelCount}")
                    appendLine()
                    appendLine("Avg Order Value: ${report.salesSummary.averageOrderValue.toInt()} MMK")
                    appendLine()
                    appendLine("═══ PARCEL FEES ═══")
                    appendLine("Total Parcel Fees: ${report.totalParcelFees.toInt()} MMK")
                    appendLine()
                    
                    if (report.parcelAdjustments.isNotEmpty()) {
                        appendLine("⚠️  MANUAL ADJUSTMENTS (${report.parcelAdjustments.size})")
                        appendLine()
                        report.parcelAdjustments.forEach { adj ->
                            val sign = if (adj.difference >= 0) "+" else ""
                            appendLine("${adj.orderNumber} - ${adj.adjustedBy}")
                            appendLine("  Default: 1,000 → Custom: ${adj.customFee.toInt()}")
                            appendLine("  Impact: $sign${adj.difference.toInt()} MMK")
                            appendLine()
                        }
                        appendLine("Total Adjustment Impact: ${if (report.totalAdjustmentImpact >= 0) "+" else ""}${report.totalAdjustmentImpact.toInt()} MMK")
                    } else {
                        appendLine("✓ No manual parcel fee adjustments")
                    }
                    
                    appendLine()
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                
                onSuccess(reportText)
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
