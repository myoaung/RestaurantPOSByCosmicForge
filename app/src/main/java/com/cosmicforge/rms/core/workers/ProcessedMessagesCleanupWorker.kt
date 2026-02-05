package com.cosmicforge.rms.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cosmicforge.rms.data.database.dao.ProcessedMessagesDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 30-Day Retention Worker
 * 
 * Cleans up old processed_messages records to prevent database bloat.
 * Runs daily at 3 AM.
 * 
 * Post-Implementation Maintenance Requirement:
 * "Execute a 30-day retention worker to purge old processed_messages records"
 */
@HiltWorker
class ProcessedMessagesCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val processedMessagesDao: ProcessedMessagesDao
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "processed_messages_cleanup"
        private const val TAG = "CleanupWorker"
        
        // 30 days in milliseconds
        private const val RETENTION_PERIOD_MS = 30L * 24 * 60 * 60 * 1000
        
        /**
         * Schedule the cleanup worker
         * Runs daily at 3 AM
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true) // Don't run if battery is low
                .build()
            
            val cleanupRequest = PeriodicWorkRequestBuilder<ProcessedMessagesCleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                cleanupRequest
            )
            
            android.util.Log.d(TAG, "✅ Cleanup worker scheduled (daily at 3 AM)")
        }
        
        /**
         * Calculate delay until next 3 AM
         */
        private fun calculateInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, 3)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                
                // If 3 AM already passed today, schedule for tomorrow
                if (timeInMillis <= now) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            return calendar.timeInMillis - now
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d(TAG, "🔄 Starting cleanup of old processed_messages...")
            
            // Calculate cutoff timestamp (30 days ago)
            val cutoffTimestamp = System.currentTimeMillis() - RETENTION_PERIOD_MS
            
            // Delete old records
            processedMessagesDao.cleanupOldMessages(cutoffTimestamp)
            
            // Get remaining count for logging
            val remainingCount = processedMessagesDao.getRecentProcessed(limit = Int.MAX_VALUE).size
            
            android.util.Log.d(TAG, "✅ Cleanup complete. Remaining records: $remainingCount")
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Cleanup failed", e)
            
            // Retry with exponential backoff
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
