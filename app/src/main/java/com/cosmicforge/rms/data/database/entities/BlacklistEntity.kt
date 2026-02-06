package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Blacklist entity for NRC-based hiring protection
 * Prevents rehiring of terminated staff with cause
 * v10.5: Security & HR Management
 */
@Entity(
    tableName = "blacklist",
    indices = [Index(value = ["nrc_number"], unique = true)]
)
data class BlacklistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "blacklist_id")
    val blacklistId: Long = 0,
    
    @ColumnInfo(name = "nrc_number")
    val nrcNumber: String, // Myanmar NRC (unique)
    
    @ColumnInfo(name = "staff_name")
    val staffName: String,
    
    @ColumnInfo(name = "reason_for_blacklist")
    val reasonForBlacklist: String,
    
    @ColumnInfo(name = "blacklisted_at")
    val blacklistedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "blacklisted_by")
    val blacklistedBy: Long // Owner userId who blacklisted
)
