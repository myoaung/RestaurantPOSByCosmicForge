package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.core.sync.SyncEngine
import com.cosmicforge.rms.data.database.dao.OrderDao
import com.cosmicforge.rms.data.database.dao.OrderDetailDao
import com.cosmicforge.rms.data.database.entities.OrderDetailEntity
import com.cosmicforge.rms.data.database.entities.OrderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for order management with real-time sync
 */
@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val orderDetailDao: OrderDetailDao,
    private val syncEngine: SyncEngine
) {
    
    /**
     * Create new order with sync
     */
    suspend fun createOrder(order: OrderEntity): Long {
        val orderId = orderDao.insertOrder(order)
        
        // Sync to all devices
        syncEngine.syncOrderCreate(order.copy(orderId = orderId))
        
        return orderId
    }
    
    /**
     * Add item to order
     */
    suspend fun addOrderDetail(detail: OrderDetailEntity): Long {
        val detailId = orderDetailDao.insertDetail(detail)
        
        // Sync detail
        syncEngine.syncOrderDetailUpdate(detail.copy(detailId = detailId))
        
        return detailId
    }
    
    /**
     * Update order
     */
    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(order)
        syncEngine.syncOrderUpdate(order)
    }
    
    /**
     * Get order by ID
     */
    suspend fun getOrderById(orderId: Long): OrderEntity? {
        return orderDao.getOrderById(orderId)
    }
    
    /**
     * Get orders by status
     */
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(status)
    }
    
    /**
     * Get today's orders
     */
    fun getTodayOrders(): Flow<List<OrderEntity>> {
        return orderDao.getTodayOrders()
    }
    
    /**
     * Get order details for an order
     */
    fun getOrderDetails(orderId: Long): Flow<List<OrderDetailEntity>> {
        return orderDetailDao.getDetailsForOrder(orderId)
    }
    
    /**
     * Calculate order total with parcel fee
     */
    suspend fun calculateOrderTotal(orderId: Long, isParcel: Boolean): Double {
        val details = orderDetailDao.getDetailsByOrderSnapshot(orderId)
        val subtotal = details.sumOf { it.totalPrice }
        
        return if (isParcel) {
            // Add packaging fee (configurable, using 1000 MMK as default)
            subtotal + PARCEL_PACKAGING_FEE
        } else {
            subtotal
        }
    }
    
    /**
     * Claim order detail (chief accountability)
     */
    suspend fun claimOrderDetail(
        detailId: Long,
        chiefId: Long,
        chiefName: String,
        claimTime: Long
    ) {
        orderDetailDao.updateClaimInfo(detailId, chiefId, chiefName, claimTime)
        
        // Status is already updated in updateClaimInfo
    }
    
    /**
     * Mark detail as ready
     */
    suspend fun markDetailReady(detailId: Long, readyTime: Long) {
        orderDetailDao.updateReadyTime(detailId, readyTime)
        orderDetailDao.updateStatus(detailId, "READY")
    }
    
    /**
     * Get order detail by ID
     */
    suspend fun getOrderDetailById(detailId: Long): OrderDetailEntity? {
        return orderDetailDao.getDetailById(detailId)
    }
    
    /**
     * Get order details snapshot
     */
    suspend fun getOrderDetailsSnapshot(orderId: Long): List<OrderDetailEntity> {
        return orderDetailDao.getDetailsByOrderSnapshot(orderId)
    }
    
    companion object {
        private const val PARCEL_PACKAGING_FEE = 1000.0 // MMK
    }
}
