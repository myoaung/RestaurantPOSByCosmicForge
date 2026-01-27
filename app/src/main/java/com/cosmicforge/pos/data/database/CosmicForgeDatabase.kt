package com.cosmicforge.pos.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmicforge.pos.data.database.dao.*
import com.cosmicforge.pos.data.database.entities.*

/**
 * Main Room Database for Cosmic Forge POS
 * Uses SQLCipher for encryption
 */
@Database(
    entities = [
        ShopConfigEntity::class,
        UserEntity::class,
        TableEntity::class,
        MenuItemEntity::class,
        OrderEntity::class,
        OrderDetailEntity::class,
        SecurityAuditEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class CosmicForgeDatabase : RoomDatabase() {
    
    abstract fun shopConfigDao(): ShopConfigDao
    abstract fun userDao(): UserDao
    abstract fun tableDao(): TableDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun orderDetailDao(): OrderDetailDao
    abstract fun securityAuditDao(): SecurityAuditDao
    
    companion object {
        const val DATABASE_NAME = "cosmic_forge_db"
        const val DATABASE_VERSION = 2
    }
}
