package com.cosmicforge.pos.data.database.dao

import androidx.room.*
import com.cosmicforge.pos.data.database.entities.ShopConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopConfigDao {
    @Query("SELECT * FROM shop_config LIMIT 1")
    fun getShopConfig(): Flow<ShopConfigEntity?>
    
    @Query("SELECT * FROM shop_config LIMIT 1")
    suspend fun getShopConfigOnce(): ShopConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopConfig(config: ShopConfigEntity)
    
    @Update
    suspend fun updateShopConfig(config: ShopConfigEntity)
    
    @Query("UPDATE shop_config SET active_modules = :modules WHERE shop_id = :shopId")
    suspend fun updateActiveModules(shopId: String, modules: String)
    
    @Query("SELECT active_modules FROM shop_config WHERE shop_id = :shopId")
    suspend fun getActiveModules(shopId: String): String?
}
