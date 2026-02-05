package com.cosmicforge.rms.data.database.dao

import androidx.room.*
import com.cosmicforge.rms.data.database.entities.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY created_at DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE order_id = :orderId")
    suspend fun getOrderById(orderId: Long): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE order_id = :orderId")
    fun observeOrder(orderId: Long): Flow<OrderEntity?>
    
    @Query("SELECT * FROM orders WHERE sync_id = :syncId LIMIT 1")
    suspend fun getOrderBySyncId(syncId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE table_id = :tableId AND status NOT IN ('COMPLETED', 'CANCELLED') LIMIT 1")
    suspend fun getActiveOrderForTable(tableId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY created_at DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE waiter_id = :waiterId AND status NOT IN ('COMPLETED', 'CANCELLED')")
    fun getActiveOrdersForWaiter(waiterId: Long): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE DATE(created_at/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY created_at DESC")
    fun getTodayOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE DATE(created_at/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY created_at DESC")
    suspend fun getTodayOrdersSnapshot(): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    fun getOrdersByDateRange(startTime: Long, endTime: Long): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getOrdersByDateRangeSnapshot(startTime: Long, endTime: Long): List<OrderEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    @Query("UPDATE orders SET status = :status, updated_at = :timestamp WHERE order_id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE orders SET payment_method = :method, payment_status = :status WHERE order_id = :orderId")
    suspend fun updatePaymentInfo(orderId: Long, method: String, status: String)
    
    @Delete
    suspend fun deleteOrder(order: OrderEntity)
}
