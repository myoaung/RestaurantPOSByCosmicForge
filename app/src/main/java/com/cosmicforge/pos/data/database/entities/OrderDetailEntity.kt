package com.cosmicforge.pos.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Order detail entity (line items) with chief accountability
 * This matches the order_details table from the spec
 */
@Entity(tableName = "order_details")
data class OrderDetailEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "detail_id")
    val detailId: Long = 0,
    
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    
    @ColumnInfo(name = "item_name")
    val itemName: String, // Cached for display
    
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,
    
    @ColumnInfo(name = "subtotal")
    val subtotal: Double,
    
    @ColumnInfo(name = "modifiers") // JSON string of applied modifiers
    val modifiers: String? = null,
    
    @ColumnInfo(name = "special_instructions")
    val specialInstructions: String? = null,
    
    // Chief accountability
    @ColumnInfo(name = "chief_id")
    val chiefId: Long? = null, // Assigned when chief "claims" the item
    
    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, COOKING, READY, SERVED
    
    @ColumnInfo(name = "prep_station")
    val prepStation: String? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long? = null, // When chief claimed it
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null, // When marked ready
    
    @ColumnInfo(name = "parcel_fee")
    val parcelFee: Double = 0.0,
    
    @ColumnInfo(name = "is_void")
    val isVoid: Boolean = false,
    
    @ColumnInfo(name = "void_by")
    val voidBy: Long? = null, // Manager ID who voided this
    
    @ColumnInfo(name = "void_reason")
    val voidReason: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COOKING = "COOKING"
        const val STATUS_READY = "READY"
        const val STATUS_SERVED = "SERVED"
    }
    
    /**
     * Calculate preparation time in seconds
     */
    fun getPrepTimeSeconds(): Long? {
        return if (startTime != null && endTime != null) {
            (endTime - startTime) / 1000
        } else null
    }
}
