package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User entity with role-based access control
 * Roles: 1=Owner, 2=Manager, 3=Waiter, 4=Chief
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "pin_hash")
    val pinHash: String, // SHA-256 hash of PIN
    
    @ColumnInfo(name = "role_level")
    val roleLevel: Int, // 1:Owner, 2:Manager, 3:Waiter, 4:Chief
    
    @ColumnInfo(name = "station_id")
    val stationId: String? = null, // e.g., 'KITCHEN_HOT', 'KITCHEN_COLD', 'BAR'
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_OWNER = 1
        const val ROLE_MANAGER = 2
        const val ROLE_WAITER = 3
        const val ROLE_CHIEF = 4
    }
    
    fun getRoleName(): String = when (roleLevel) {
        ROLE_OWNER -> "Owner"
        ROLE_MANAGER -> "Manager"
        ROLE_WAITER -> "Waiter"
        ROLE_CHIEF -> "Chief"
        else -> "Unknown"
    }
}
