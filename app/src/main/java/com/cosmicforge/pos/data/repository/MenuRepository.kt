package com.cosmicforge.pos.data.repository

import com.cosmicforge.pos.data.database.dao.MenuItemDao
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for menu management
 */
@Singleton
class MenuRepository @Inject constructor(
    private val menuItemDao: MenuItemDao
) {
    
    /**
     * Get all menu items
     */
    fun getAllMenuItems(): Flow<List<MenuItemEntity>> {
        return menuItemDao.getAllMenuItems()
    }
    
    /**
     * Get available menu items
     */
    fun getAvailableMenuItems(): Flow<List<MenuItemEntity>> {
        return menuItemDao.getAvailableMenuItems()
    }
    
    /**
     * Get items by category
     */
    fun getMenuItemsByCategory(category: String): Flow<List<MenuItemEntity>> {
        return menuItemDao.getMenuItemsByCategory(category)
    }
    
    /**
     * Get all categories
     */
    fun getAllCategories(): Flow<List<String>> {
        return menuItemDao.getAllCategories()
    }
    
    /**
     * Get item by ID
     */
    suspend fun getMenuItemById(itemId: Long): MenuItemEntity? {
        return menuItemDao.getMenuItemById(itemId)
    }
    
    /**
     * Search menu items
     */
    fun searchMenuItems(query: String): Flow<List<MenuItemEntity>> {
        return menuItemDao.searchMenuItems(query)
    }
    
    /**
     * Get items by prep station
     */
    fun getMenuItemsByPrepStation(station: String): Flow<List<MenuItemEntity>> {
        return menuItemDao.getMenuItemsByPrepStation(station)
    }
}
