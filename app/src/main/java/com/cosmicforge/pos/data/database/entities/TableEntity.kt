package com.cosmicforge.pos.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Table entity for floor map management
 */
@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_id")
    val tableId: String, // e.g., "T-01", "T-02"
    
    @ColumnInfo(name = "table_name")
    val tableName: String,
    
    @ColumnInfo(name = "capacity")
    val capacity: Int,
    
    @ColumnInfo(name = "position_x")
    val positionX: Float, // Grid position for floor map
    
    @ColumnInfo(name = "position_y")
    val positionY: Float,
    
    @ColumnInfo(name = "status")
    val status: String, // FREE, IN_USE, DIRTY, RESERVED
    
    @ColumnInfo(name = "current_order_id")
    val currentOrderId: Long? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_FREE = "FREE"
        const val STATUS_IN_USE = "IN_USE"
        const val STATUS_DIRTY = "DIRTY"
        const val STATUS_RESERVED = "RESERVED"
    }
}
