package com.cosmicforge.pos.data.database.dao

import androidx.room.*
import com.cosmicforge.pos.data.database.entities.OrderDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDetailDao {
    @Query("SELECT * FROM order_details WHERE order_id = :orderId AND is_void = 0 ORDER BY detail_id")
    fun getDetailsForOrder(orderId: Long): Flow<List<OrderDetailEntity>>
    
    @Query("SELECT * FROM order_details WHERE detail_id = :detailId")
    suspend fun getDetailById(detailId: Long): OrderDetailEntity?
    
    @Query("SELECT * FROM order_details WHERE status = :status AND is_void = 0 ORDER BY created_at")
    fun getDetailsByStatus(status: String): Flow<List<OrderDetailEntity>>
    
    @Query("SELECT * FROM order_details WHERE prep_station = :station AND status IN ('PENDING', 'COOKING') AND is_void = 0 ORDER BY created_at")
    fun getPendingDetailsForStation(station: String): Flow<List<OrderDetailEntity>>
    
    @Query("SELECT * FROM order_details WHERE chief_id = :chiefId AND is_void = 0")
    fun getDetailsForChief(chiefId: Long): Flow<List<OrderDetailEntity>>
    
    @Query("""
        SELECT * FROM order_details 
        WHERE DATE(created_at/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') 
        AND chief_id = :chiefId 
        AND is_void = 0
    """)
    fun getTodayDetailsForChief(chiefId: Long): Flow<List<OrderDetailEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: OrderDetailEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<OrderDetailEntity>)
    
    @Update
    suspend fun updateDetail(detail: OrderDetailEntity)
    
    @Query("UPDATE order_details SET status = :status, updated_at = :timestamp WHERE detail_id = :detailId")
    suspend fun updateStatus(detailId: Long, status: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE order_details SET chief_id = :chiefId, status = 'COOKING', start_time = :startTime WHERE detail_id = :detailId")
    suspend fun claimByChief(detailId: Long, chiefId: Long, startTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE order_details SET status = 'READY', end_time = :endTime WHERE detail_id = :detailId")
    suspend fun markReady(detailId: Long, endTime: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE order_details 
        SET claimed_by_id = :chiefId, 
            claimed_by = :chiefName, 
            claimed_at = :claimTime,
            chief_id = :chiefId,
            start_time = :claimTime,
            status = 'COOKING',
            updated_at = :claimTime
        WHERE detail_id = :detailId
    """)
    suspend fun updateClaimInfo(
        detailId: Long, 
        chiefId: Long, 
        chiefName: String, 
        claimTime: Long
    )
    
    @Query("UPDATE order_details SET is_void = 1, void_by = :managerId, void_reason = :reason WHERE detail_id = :detailId")
    suspend fun voidDetail(detailId: Long, managerId: Long, reason: String)
    
    @Delete
    suspend fun deleteDetail(detail: OrderDetailEntity)
    
    @Query("DELETE FROM order_details WHERE order_id = :orderId")
    suspend fun deleteDetailsForOrder(orderId: Long)
}
