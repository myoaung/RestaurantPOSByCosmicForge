package com.cosmicforge.pos.core.backup

import android.content.Context
import android.util.Log
import com.cosmicforge.pos.data.database.dao.OrderDao
import com.cosmicforge.pos.data.database.dao.SecurityAuditDao
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud backup manager for daily data sync
 * Triggers once daily when internet is available
 */
@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val orderDao: OrderDao,
    private val securityAuditDao: SecurityAuditDao,
    private val gson: Gson
) {
    
    private val backupDir = File(context.filesDir, "backups")
    
    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }
    
    /**
     * Perform daily backup
     * Returns backup file path if successful
     */
    suspend fun performDailyBackup(): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "backup_$today.json")
                
                // Check if already backed up today
                if (backupFile.exists()) {
                    Log.d(TAG, "Backup already exists for today")
                    return@withContext BackupResult.AlreadyExists(backupFile.absolutePath)
                }
                
                // Collect data to backup
                val backupData = BackupData(
                    date = today,
                    timestamp = System.currentTimeMillis(),
                    orders = orderDao.getTodayOrdersSnapshot(),
                    audits = securityAuditDao.getTodayAuditsSnapshot()
                )
                
                // Serialize to JSON
                val json = gson.toJson(backupData)
                
                // Write to file
                backupFile.writeText(json)
                
                Log.d(TAG, "Backup created: ${backupFile.absolutePath}")
                Log.d(TAG, "Orders: ${backupData.orders.size}, Audits: ${backupData.audits.size}")
                
                BackupResult.Success(
                    filePath = backupFile.absolutePath,
                    orderCount = backupData.orders.size,
                    auditCount = backupData.audits.size,
                    fileSizeKb = backupFile.length() / 1024
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                BackupResult.Failure(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Upload backup to cloud (when internet available)
     * Placeholder for actual cloud implementation
     */
    suspend fun uploadToCloud(filePath: String): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext UploadResult.Failure("File not found")
                }
                
                // TODO: Implement actual cloud upload
                // Example: Firebase Storage, AWS S3, or custom server
                
                // For now, simulate upload delay
                kotlinx.coroutines.delay(2000)
                
                Log.d(TAG, "Backup uploaded to cloud: ${file.name}")
                
                UploadResult.Success(
                    fileName = file.name,
                    cloudUrl = "cloud://backups/${file.name}" // Placeholder
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                UploadResult.Failure(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Check if internet is available
     */
    fun isInternetAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get list of local backups
     */
    fun getLocalBackups(): List<BackupFileInfo> {
        return backupDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { file ->
                BackupFileInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeKb = file.length() / 1024,
                    createdAt = file.lastModified()
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }
    
    /**
     * Clean old backups (keep last 30 days)
     */
    suspend fun cleanOldBackups() {
        withContext(Dispatchers.IO) {
            try {
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                backupDir.listFiles()
                    ?.filter { it.lastModified() < thirtyDaysAgo }
                    ?.forEach { file ->
                        file.delete()
                        Log.d(TAG, "Deleted old backup: ${file.name}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean old backups", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "CloudBackup"
    }
}

/**
 * Backup data structure
 */
data class BackupData(
    val date: String,
    val timestamp: Long,
    val orders: List<com.cosmicforge.pos.data.database.entities.OrderEntity>,
    val audits: List<com.cosmicforge.pos.data.database.entities.SecurityAuditEntity>
)

/**
 * Backup result
 */
sealed class BackupResult {
    data class Success(
        val filePath: String,
        val orderCount: Int,
        val auditCount: Int,
        val fileSizeKb: Long
    ) : BackupResult()
    
    data class AlreadyExists(val filePath: String) : BackupResult()
    data class Failure(val reason: String) : BackupResult()
}

/**
 * Upload result
 */
sealed class UploadResult {
    data class Success(val fileName: String, val cloudUrl: String) : UploadResult()
    data class Failure(val reason: String) : UploadResult()
}

/**
 * Backup file info
 */
data class BackupFileInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeKb: Long,
    val createdAt: Long
)
