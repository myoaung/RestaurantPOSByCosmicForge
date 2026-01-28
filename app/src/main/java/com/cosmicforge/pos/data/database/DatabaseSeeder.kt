package com.cosmicforge.pos.data.database

import com.cosmicforge.pos.data.database.dao.MenuItemDao
import com.cosmicforge.pos.data.database.dao.TableDao
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import com.cosmicforge.pos.data.database.entities.TableEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database seeder for Phase 3 mock data
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val tableDao: TableDao,
    private val menuItemDao: MenuItemDao
) {
    
    /**
     * Seed initial test data
     */
    suspend fun seedMockData() {
        // Check if data already exists
        val existingTables = tableDao.getAllActiveTables()
        if (existingTables.isNotEmpty()) {
            return // Already seeded
        }
        
        // Seed tables
        seedTables()
        
        // Seed menu items
        seedMenuItems()
    }
    
    private suspend fun seedTables() {
        val tables = listOf(
            TableEntity(
                tableNumber = "T-01",
                capacity = 2,
                shape = "Circle",
                status = "AVAILABLE",
                positionX = 100f,
                positionY = 100f
            ),
            TableEntity(
                tableNumber = "T-02",
                capacity = 4,
                shape = "Square",
                status = "OCCUPIED",
                positionX = 300f,
                positionY = 100f,
                currentOrderId = 1L // Mock order
            ),
            TableEntity(
                tableNumber = "T-03",
                capacity = 4,
                shape = "Square",
                status = "AVAILABLE",
                positionX = 500f,
                positionY = 100f
            ),
            TableEntity(
                tableNumber = "B-01",
                capacity = 6,
                shape = "Rectangle",
                status = "AVAILABLE",
                positionX = 100f,
                positionY = 300f
            ),
            TableEntity(
                tableNumber = "P-01",
                capacity = 2,
                shape = "Circle",
                status = "DIRTY",
                positionX = 500f,
                positionY = 300f
            )
        )
        
        tables.forEach { table ->
            tableDao.insertTable(table)
        }
    }
    
    private suspend fun seedMenuItems() {
        val menuItems = listOf(
            MenuItemEntity(
                itemName = "Cosmic Espresso",
                itemNameMm = "ကော့စမစ် အက်စပရက်ဆို",
                category = "Drinks",
                price = 4.50,
                taxRate = 0.05,
                prepStation = "BAR",
                isAvailable = true
            ),
            MenuItemEntity(
                itemName = "Nebula Burger",
                itemNameMm = "နီဗျူလာ ဘာဂါ",
                category = "Mains",
                price = 12.00,
                taxRate = 0.08,
                prepStation = "GRILL",
                isAvailable = true
            ),
            MenuItemEntity(
                itemName = "Starry Fries",
                itemNameMm = "ကြယ်စင်း ကြော်",
                category = "Sides",
                price = 5.50,
                taxRate = 0.08,
                prepStation = "FRY",
                isAvailable = true
            ),
            MenuItemEntity(
                itemName = "Black Hole Cake",
                itemNameMm = "တွင်းနက် ကိတ်မုန့်",
                category = "Desserts",
                price = 7.00,
                taxRate = 0.08,
                prepStation = "PASTRY",
                isAvailable = false // Out of stock
            )
        )
        
        menuItems.forEach { item ->
            menuItemDao.insertItem(item)
        }
    }
}
