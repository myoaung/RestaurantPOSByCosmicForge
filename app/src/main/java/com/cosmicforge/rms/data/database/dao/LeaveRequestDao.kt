package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.LeaveRequestEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for leave request management
 * v10.6: Leave Management System
 */
@Dao
interface LeaveRequestDao {
    
    @Query("SELECT * FROM leave_requests ORDER BY created_at DESC")
    fun getAllRequests(): Flow<List<LeaveRequestEntity>>
    
    @Query("SELECT * FROM leave_requests WHERE status = :status ORDER BY created_at DESC")
    fun getRequestsByStatus(status: String): Flow<List<LeaveRequestEntity>>
    
    @Query("SELECT * FROM leave_requests WHERE user_id = :userId ORDER BY created_at DESC")
    fun getRequestsByUser(userId: Long): Flow<List<LeaveRequestEntity>>
    
    @Query("SELECT * FROM leave_requests WHERE status = '${LeaveRequestEntity.STATUS_PENDING}' ORDER BY created_at ASC")
    fun getPendingRequests(): Flow<List<LeaveRequestEntity>>
    
    @Query("SELECT * FROM leave_requests WHERE request_id = :requestId")
    suspend fun getRequestById(requestId: Long): LeaveRequestEntity?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRequest(request: LeaveRequestEntity): Long
    
    @Update
    suspend fun updateRequest(request: LeaveRequestEntity)
    
    @Query("UPDATE leave_requests SET status = :status, reviewed_by = :reviewedBy, reviewed_at = :reviewedAt WHERE request_id = :requestId")
    suspend fun approveOrRejectRequest(requestId: Long, status: String, reviewedBy: Long, reviewedAt: Long)
    
    @Delete
    suspend fun deleteRequest(request: LeaveRequestEntity)
}
