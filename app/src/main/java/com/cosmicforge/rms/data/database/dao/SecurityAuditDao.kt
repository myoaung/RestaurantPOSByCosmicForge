package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.SecurityAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityAuditDao {
    
    @Query("SELECT * FROM security_audit ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAudits(limit: Int = 100): Flow<List<SecurityAuditEntity>>
    
    @Query("SELECT * FROM security_audit WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAuditsForUser(userId: Long): Flow<List<SecurityAuditEntity>>
    
    @Query("SELECT * FROM security_audit WHERE action_type = :actionType ORDER BY timestamp DESC")
    fun getAuditsByAction(actionType: String): Flow<List<SecurityAuditEntity>>
    
    @Query("""
        SELECT * FROM security_audit 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        ORDER BY timestamp DESC
    """)
    fun getAuditsByDateRange(startTime: Long, endTime: Long): Flow<List<SecurityAuditEntity>>
    
    @Query("""
        SELECT * FROM security_audit 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        ORDER BY timestamp DESC
    """)
    fun getTodayAudits(): Flow<List<SecurityAuditEntity>>
    
    @Query("""
        SELECT * FROM security_audit 
        WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        ORDER BY timestamp DESC
    """)
    suspend fun getTodayAuditsSnapshot(): List<SecurityAuditEntity>
    
    @Query("""
        SELECT * FROM security_audit 
        WHERE action_type IN ('VOID_ITEM', 'VOID_ORDER', 'REFUND', 'MANAGER_OVERRIDE')
        ORDER BY timestamp DESC
    """)
    fun getOverrideAudits(): Flow<List<SecurityAuditEntity>>
    
    @Query("SELECT * FROM security_audit WHERE was_successful = 0 ORDER BY timestamp DESC")
    fun getFailedActions(): Flow<List<SecurityAuditEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: SecurityAuditEntity): Long
    
    @Query("DELETE FROM security_audit WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAudits(cutoffTime: Long): Int
}
