package com.cosmicforge.rms.data.database

import com.cosmicforge.rms.data.database.entities.MenuItemEntity
import com.cosmicforge.rms.data.database.entities.TableEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database seeder for Phase 3 mock data
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: CosmicForgeDatabase
) {
    
    /**
     * Seed initial test data
     */
    suspend fun seedMockData() {
        // Check if data already exists
        val existingTables = database.tableDao().getAllActiveTables().first()
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
                tableId = "T-01",
                tableName = "Table 1",
                capacity = 4,
                status = TableEntity.STATUS_FREE,
                positionX = 100f,
                positionY = 100f
            ),
            TableEntity(
                tableId = "T-02",
                tableName = "Table 2",
                capacity = 2,
                status = TableEntity.STATUS_IN_USE,
                positionX = 300f,
                positionY = 100f,
                currentOrderId = 1L // Mock order
            ),
            TableEntity(
                tableId = "T-03",
                tableName = "Table 3",
                capacity = 4,
                status = TableEntity.STATUS_FREE,
                positionX = 500f,
                positionY = 100f
            ),
            TableEntity(
                tableId = "B-01",
                tableName = "Bar 1",
                capacity = 6,
                status = TableEntity.STATUS_FREE,
                positionX = 100f,
                positionY = 300f
            ),
            TableEntity(
                tableId = "P-01",
                tableName = "Patio 1",
                capacity = 2,
                status = TableEntity.STATUS_DIRTY,
                positionX = 500f,
                positionY = 300f
            )
        )
        
        tables.forEach { table ->
            database.tableDao().insertTable(table)
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
            database.menuItemDao().insertItem(item)
        }
    }
}
