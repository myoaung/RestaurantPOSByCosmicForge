package com.cosmicforge.pos.data.database.dao

import androidx.room.*
import com.cosmicforge.pos.data.database.entities.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE is_active = 1 ORDER BY table_id")
    fun getAllActiveTables(): Flow<List<TableEntity>>
    
    @Query("SELECT * FROM tables WHERE table_id = :tableId")
    suspend fun getTableById(tableId: String): TableEntity?
    
    @Query("SELECT * FROM tables WHERE table_id = :tableId")
    fun observeTable(tableId: String): Flow<TableEntity?>
    
    @Query("SELECT * FROM tables WHERE status = :status AND is_active = 1")
    fun getTablesByStatus(status: String): Flow<List<TableEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<TableEntity>)
    
    @Update
    suspend fun updateTable(table: TableEntity)
    
    @Query("UPDATE tables SET status = :status, updated_at = :timestamp WHERE table_id = :tableId")
    suspend fun updateTableStatus(tableId: String, status: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE tables SET current_order_id = :orderId WHERE table_id = :tableId")
    suspend fun assignOrderToTable(tableId: String, orderId: Long?)
    
    @Delete
    suspend fun deleteTable(table: TableEntity)
}
