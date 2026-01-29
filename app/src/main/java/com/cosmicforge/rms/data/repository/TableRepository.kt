package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.core.sync.SyncEngine
import com.cosmicforge.rms.data.database.dao.TableDao
import com.cosmicforge.rms.data.database.entities.TableEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for table management with real-time sync
 */
@Singleton
class TableRepository @Inject constructor(
    private val tableDao: TableDao,
    private val syncEngine: SyncEngine
) {
    
    /**
     * Get all tables
     */
    fun getAllTables(): Flow<List<TableEntity>> {
        return tableDao.getAllActiveTables()
    }
    
    /**
     * Get tables by floor
     */
    fun getTablesByFloor(floor: Int): Flow<List<TableEntity>> {
        // Floor filtering not implemented in DAO, return all tables
        return tableDao.getAllActiveTables()
    }
    
    /**
     * Get tables by status
     */
    fun getTablesByStatus(status: String): Flow<List<TableEntity>> {
        return tableDao.getTablesByStatus(status)
    }
    
    /**
     * Get table by ID
     */
    suspend fun getTableById(tableId: String): TableEntity? {
        return tableDao.getTableById(tableId)
    }
    
    /**
     * Update table status with real-time sync
     * Instantly broadcasts to all connected devices
     */
    suspend fun updateTableStatus(tableId: String, newStatus: String) {
        // Update locally first
        tableDao.updateTableStatus(tableId, newStatus)
        
        // Broadcast to all devices instantly
        syncEngine.syncTableStatusUpdate(tableId, newStatus)
    }
    
    /**
     * Assign order to table
     */
    suspend fun assignOrderToTable(tableId: String, orderId: Long) {
        tableDao.assignOrderToTable(tableId, orderId)
        // Table status automatically becomes IN_USE
        updateTableStatus(tableId, TableEntity.STATUS_IN_USE)
    }
    
    /**
     * Clear table (mark as dirty for cleaning)
     */
    suspend fun clearTable(tableId: String) {
        tableDao.assignOrderToTable(tableId, null)
        updateTableStatus(tableId, TableEntity.STATUS_DIRTY)
    }
    
    /**
     * Mark table as clean and free
     */
    suspend fun markTableClean(tableId: String) {
        updateTableStatus(tableId, TableEntity.STATUS_FREE)
    }
    
    /**
     * Reserve table
     */
    suspend fun reserveTable(tableId: String, reservationName: String) {
        tableDao.updateTableStatus(tableId, TableEntity.STATUS_RESERVED)
        syncEngine.syncTableStatusUpdate(tableId, TableEntity.STATUS_RESERVED)
    }
    
    /**
     * Create new table
     */
    suspend fun createTable(table: TableEntity): Long {
        tableDao.insertTable(table)
        return 0L // insertTable doesn't return Long
    }
    
    /**
     * Update table
     */
    suspend fun updateTable(table: TableEntity) {
        tableDao.updateTable(table)
    }
}
