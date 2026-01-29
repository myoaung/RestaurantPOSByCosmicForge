package com.cosmicforge.rms.core.stress

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stress test monitor for beta deployment
 * Tracks system performance metrics
 */
@Singleton
class StressTestMonitor @Inject constructor() {
    
    private val _metrics = MutableStateFlow(StressTestMetrics())
    val metrics: StateFlow<StressTestMetrics> = _metrics.asStateFlow()
    
    private var startTime = System.currentTimeMillis()
    
    /**
     * Record order creation
     */
    fun recordOrderCreation(processingTimeMs: Long) {
        _metrics.value = _metrics.value.copy(
            totalOrders = _metrics.value.totalOrders + 1,
            avgOrderProcessingMs = calculateRunningAverage(
                _metrics.value.avgOrderProcessingMs,
                processingTimeMs,
                _metrics.value.totalOrders
            ),
            maxOrderProcessingMs = maxOf(_metrics.value.maxOrderProcessingMs, processingTimeMs)
        )
        
        if (processingTimeMs > SLOW_ORDER_THRESHOLD_MS) {
            recordSlowOperation("Order Creation", processingTimeMs)
        }
    }
    
    /**
     * Record sync operation
     */
    fun recordSyncOperation(deviceCount: Int, syncTimeMs: Long) {
        _metrics.value = _metrics.value.copy(
            totalSyncOperations = _metrics.value.totalSyncOperations + 1,
            avgSyncTimeMs = calculateRunningAverage(
                _metrics.value.avgSyncTimeMs,
                syncTimeMs,
                _metrics.value.totalSyncOperations
            ),
            maxSyncTimeMs = maxOf(_metrics.value.maxSyncTimeMs, syncTimeMs),
            maxConnectedDevices = maxOf(_metrics.value.maxConnectedDevices, deviceCount)
        )
        
        if (syncTimeMs > SLOW_SYNC_THRESHOLD_MS) {
            recordSlowOperation("Sync ($deviceCount devices)", syncTimeMs)
        }
    }
    
    /**
     * Record database query
     */
    fun recordDatabaseQuery(queryTimeMs: Long) {
        _metrics.value = _metrics.value.copy(
            totalDbQueries = _metrics.value.totalDbQueries + 1,
            avgDbQueryMs = calculateRunningAverage(
                _metrics.value.avgDbQueryMs,
                queryTimeMs,
                _metrics.value.totalDbQueries
            ),
            maxDbQueryMs = maxOf(_metrics.value.maxDbQueryMs, queryTimeMs)
        )
        
        if (queryTimeMs > SLOW_QUERY_THRESHOLD_MS) {
            recordSlowOperation("Database Query", queryTimeMs)
        }
    }
    
    /**
     * Record memory usage
     */
    fun recordMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        
        _metrics.value = _metrics.value.copy(
            currentMemoryMb = usedMemoryMb,
            maxMemoryMb = maxOf(_metrics.value.maxMemoryMb, usedMemoryMb)
        )
        
        if (usedMemoryMb > HIGH_MEMORY_THRESHOLD_MB) {
            Log.w(TAG, "High memory usage: ${usedMemoryMb}MB")
        }
    }
    
    /**
     * Record crash or error
     */
    fun recordError(errorType: String, errorMessage: String) {
        _metrics.value = _metrics.value.copy(
            errorCount = _metrics.value.errorCount + 1,
            lastError = "$errorType: $errorMessage"
        )
        
        Log.e(TAG, "Error recorded: $errorType - $errorMessage")
    }
    
    /**
     * Record slow operation
     */
    private fun recordSlowOperation(operation: String, timeMs: Long) {
        val slowOp = SlowOperation(operation, timeMs, System.currentTimeMillis())
        val updatedList = (_metrics.value.slowOperations + slowOp).takeLast(10)
        
        _metrics.value = _metrics.value.copy(
            slowOperations = updatedList
        )
        
        Log.w(TAG, "Slow operation: $operation took ${timeMs}ms")
    }
    
    /**
     * Calculate running average
     */
    private fun calculateRunningAverage(currentAvg: Long, newValue: Long, count: Int): Long {
        return ((currentAvg * (count - 1)) + newValue) / count
    }
    
    /**
     * Get uptime
     */
    fun getUptimeHours(): Double {
        return (System.currentTimeMillis() - startTime) / (1000.0 * 60 * 60)
    }
    
    /**
     * Generate stress test report
     */
    fun generateReport(): String {
        val m = _metrics.value
        val uptimeHours = getUptimeHours()
        
        return buildString {
            appendLine("═══ STRESS TEST REPORT ═══")
            appendLine()
            appendLine("Uptime: ${String.format("%.1f", uptimeHours)} hours")
            appendLine()
            
            appendLine("ORDERS:")
            appendLine("  Total: ${m.totalOrders}")
            appendLine("  Avg Processing: ${m.avgOrderProcessingMs}ms")
            appendLine("  Max Processing: ${m.maxOrderProcessingMs}ms")
            appendLine()
            
            appendLine("SYNC:")
            appendLine("  Operations: ${m.totalSyncOperations}")
            appendLine("  Avg Time: ${m.avgSyncTimeMs}ms")
            appendLine("  Max Time: ${m.maxSyncTimeMs}ms")
            appendLine("  Max Devices: ${m.maxConnectedDevices}")
            appendLine()
            
            appendLine("DATABASE:")
            appendLine("  Queries: ${m.totalDbQueries}")
            appendLine("  Avg Query: ${m.avgDbQueryMs}ms")
            appendLine("  Max Query: ${m.maxDbQueryMs}ms")
            appendLine()
            
            appendLine("MEMORY:")
            appendLine("  Current: ${m.currentMemoryMb}MB")
            appendLine("  Peak: ${m.maxMemoryMb}MB")
            appendLine()
            
            appendLine("RELIABILITY:")
            appendLine("  Errors: ${m.errorCount}")
            if (m.lastError != null) {
                appendLine("  Last Error: ${m.lastError}")
            }
            
            if (m.slowOperations.isNotEmpty()) {
                appendLine()
                appendLine("SLOW OPERATIONS:")
                m.slowOperations.forEach { op ->
                    appendLine("  ${op.operation}: ${op.durationMs}ms")
                }
            }
            
            appendLine()
            appendLine("═══════════════════════════")
        }
    }
    
    /**
     * Reset metrics
     */
    fun reset() {
        startTime = System.currentTimeMillis()
        _metrics.value = StressTestMetrics()
        Log.d(TAG, "Stress test metrics reset")
    }
    
    companion object {
        private const val TAG = "StressTest"
        private const val SLOW_ORDER_THRESHOLD_MS = 1000L
        private const val SLOW_SYNC_THRESHOLD_MS = 500L
        private const val SLOW_QUERY_THRESHOLD_MS = 100L
        private const val HIGH_MEMORY_THRESHOLD_MB = 200L
    }
}

/**
 * Stress test metrics
 */
data class StressTestMetrics(
    val totalOrders: Int = 0,
    val avgOrderProcessingMs: Long = 0,
    val maxOrderProcessingMs: Long = 0,
    
    val totalSyncOperations: Int = 0,
    val avgSyncTimeMs: Long = 0,
    val maxSyncTimeMs: Long = 0,
    val maxConnectedDevices: Int = 0,
    
    val totalDbQueries: Int = 0,
    val avgDbQueryMs: Long = 0,
    val maxDbQueryMs: Long = 0,
    
    val currentMemoryMb: Long = 0,
    val maxMemoryMb: Long = 0,
    
    val errorCount: Int = 0,
    val lastError: String? = null,
    
    val slowOperations: List<SlowOperation> = emptyList()
)

/**
 * Slow operation record
 */
data class SlowOperation(
    val operation: String,
    val durationMs: Long,
    val timestamp: Long
)
