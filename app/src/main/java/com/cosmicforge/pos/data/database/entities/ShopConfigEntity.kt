package com.cosmicforge.pos.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Shop configuration including licensing and active modules
 * Maps to shop_config table in the spec
 */
@Entity(tableName = "shop_config")
data class ShopConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "shop_id")
    val shopId: String,
    
    @ColumnInfo(name = "shop_name")
    val shopName: String,
    
    @ColumnInfo(name = "active_modules")
    val activeModules: String, // Comma-separated: "CORE,FLOOR,PARCEL,CHIEF_AUDIT,SMS"
    
    @ColumnInfo(name = "license_key_hash")
    val licenseKeyHash: String,
    
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long, // Timestamp
    
    @ColumnInfo(name = "grace_period_end")
    val gracePeriodEnd: Long, // Timestamp
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
