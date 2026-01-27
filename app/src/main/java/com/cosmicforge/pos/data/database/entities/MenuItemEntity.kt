package com.cosmicforge.pos.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Menu item entity with category and modifiers support
 */
@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    val itemId: Long = 0,
    
    @ColumnInfo(name = "item_name")
    val itemName: String,
    
    @ColumnInfo(name = "item_name_mm") // Myanmar language support
    val itemNameMm: String? = null,
    
    @ColumnInfo(name = "category")
    val category: String, // e.g., "APPETIZER", "MAIN", "DESSERT", "BEVERAGE"
    
    @ColumnInfo(name = "price")
    val price: Double,
    
    @ColumnInfo(name = "cost")
    val cost: Double = 0.0, // For profit margin calculation
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    
    @ColumnInfo(name = "available_modifiers") // JSON array of modifier IDs
    val availableModifiers: String? = null,
    
    @ColumnInfo(name = "prep_station") // Which kitchen station prepares this
    val prepStation: String? = null, // e.g., "KITCHEN_HOT", "BAR"
    
    @ColumnInfo(name = "is_available")
    val isAvailable: Boolean = true,
    
    @ColumnInfo(name = "is_featured")
    val isFeatured: Boolean = false,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
