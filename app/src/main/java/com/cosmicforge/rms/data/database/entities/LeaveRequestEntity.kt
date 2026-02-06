package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Leave request entity for staff time-off management
 * Supports sick leave, annual leave, and emergency leave tracking
 * v10.6: Leave Management System
 */
@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "request_id")
    val requestId: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "leave_type")
    val leaveType: String, // "Sick", "Annual", "Emergency"
    
    @ColumnInfo(name = "start_date")
    val startDate: Long, // Timestamp
    
    @ColumnInfo(name = "end_date")
    val endDate: Long, // Timestamp
    
    @ColumnInfo(name = "reason")
    val reason: String,
    
    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING, // "Pending", "Approved", "Rejected"
    
    @ColumnInfo(name = "reviewed_by")
    val reviewedBy: Long? = null, // Manager/Owner userId
    
    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: Long? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_APPROVED = "Approved"
        const val STATUS_REJECTED = "Rejected"
        
        const val TYPE_SICK = "Sick"
        const val TYPE_ANNUAL = "Annual"
        const val TYPE_EMERGENCY = "Emergency"
    }
}
