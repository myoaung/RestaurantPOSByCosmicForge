package com.cosmicforge.rms.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Order entity (header)
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order_id")
    val orderId: Long = 0,
    
    @ColumnInfo(name = "order_number")
    val orderNumber: String, // Human-readable order number
    
    @ColumnInfo(name = "order_type")
    val orderType: String, // DINE_IN, PARCEL
    
    @ColumnInfo(name = "table_id")
    val tableId: String? = null, // Null for parcel orders
    
    @ColumnInfo(name = "customer_name")
    val customerName: String? = null, // For parcel/delivery
    
    @ColumnInfo(name = "customer_phone")
    val customerPhone: String? = null,
    
    @ColumnInfo(name = "waiter_name")
    val waiterName: String? = null, // Waiter who created the order (cached for historical accuracy)
    
    @ColumnInfo(name = "waiter_id")
    val waiterId: Long,
    
    @ColumnInfo(name = "status")
    val status: String, // PENDING, IN_PROGRESS, READY, COMPLETED, CANCELLED
    
    @ColumnInfo(name = "subtotal")
    val subtotal: Double = 0.0,
    
    @ColumnInfo(name = "parcel_fee")
    val parcelFee: Double = 0.0,
    
    @ColumnInfo(name = "custom_parcel_fee")
    val customParcelFee: Double? = null, // Manager override for parcel fee
    
    @ColumnInfo(name = "parcel_fee_override_by")
    val parcelFeeOverrideBy: String? = null, // Waiter/Manager who set custom fee
    
    @ColumnInfo(name = "tax")
    val tax: Double = 0.0,
    
    @ColumnInfo(name = "discount")
    val discount: Double = 0.0,
    
    @ColumnInfo(name = "total")
    val total: Double = 0.0,
    
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0, // Alias for total (for UI compatibility and historical snapshots)
    
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String? = null, // CASH, KPAY, CBPAY
    
    @ColumnInfo(name = "payment_status")
    val paymentStatus: String = "UNPAID", // UNPAID, PAID, PARTIAL
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    
    // Sync metadata
    @ColumnInfo(name = "sync_id")
    val syncId: String, // UUID for P2P sync
    
    @ColumnInfo(name = "device_id")
    val deviceId: String, // Which device created this order
    
    @ColumnInfo(name = "version")
    val version: Long = 1 // For conflict resolution
) {
    companion object {
        const val TYPE_DINE_IN = "DINE_IN"
        const val TYPE_PARCEL = "PARCEL"
        
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_READY = "READY"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"
        
        const val PAYMENT_CASH = "CASH"
        const val PAYMENT_KPAY = "KPAY"
        const val PAYMENT_CBPAY = "CBPAY"
    }
}
