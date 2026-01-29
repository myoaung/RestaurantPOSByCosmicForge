package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items WHERE is_available = 1 ORDER BY sort_order, item_name")
    fun getAllAvailableItems(): Flow<List<MenuItemEntity>>
    
    @Query("SELECT * FROM menu_items WHERE item_id = :itemId")
    suspend fun getItemById(itemId: Long): MenuItemEntity?
    
    @Query("SELECT * FROM menu_items WHERE category = :category AND is_available = 1 ORDER BY sort_order")
    fun getItemsByCategory(category: String): Flow<List<MenuItemEntity>>
    
    @Query("SELECT DISTINCT category FROM menu_items WHERE is_available = 1 ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
    
    @Query("SELECT * FROM menu_items WHERE is_featured = 1 AND is_available = 1")
    fun getFeaturedItems(): Flow<List<MenuItemEntity>>
    
    @Query("SELECT * FROM menu_items WHERE prep_station = :station AND is_available = 1")
    fun getItemsByPrepStation(station: String): Flow<List<MenuItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MenuItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MenuItemEntity>)
    
    @Update
    suspend fun updateItem(item: MenuItemEntity)
    
    @Query("UPDATE menu_items SET is_available = :available WHERE item_id = :itemId")
    suspend fun updateAvailability(itemId: Long, available: Boolean)
    
    @Delete
    suspend fun deleteItem(item: MenuItemEntity)
    
    @Query("DELETE FROM menu_items")
    suspend fun deleteAllItems()
}
