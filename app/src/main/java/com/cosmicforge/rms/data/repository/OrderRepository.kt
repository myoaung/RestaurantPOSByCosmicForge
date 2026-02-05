package com.cosmicforge.rms.data.repository

import com.cosmicforge.rms.core.sync.SyncEngine
import com.cosmicforge.rms.data.database.dao.OrderDao
import com.cosmicforge.rms.data.database.dao.OrderDetailDao
import com.cosmicforge.rms.data.database.entities.OrderDetailEntity
import com.cosmicforge.rms.data.database.entities.OrderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

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
     * Create new order with sync (CRASH-PROOF VERSION)
     * Local-first: Write to Stone Vault BEFORE attempting network broadcast
     */
    suspend fun createOrder(order: OrderEntity): Long {
        return try {
            // 1. LOCAL WRITE FIRST (Antigravity Protocol)
            val orderId = orderDao.insertOrder(order)
            
            // 2. BACKGROUND SYNC (Non-blocking, error-safe)
            try {
                syncEngine.syncOrderCreate(order.copy(orderId = orderId))
            } catch (syncError: Exception) {
                Log.e("OrderRepository", "⚠️ Sync failed, queued for retry: ${syncError.message}")
                // Sync engine already handles queueing via SyncQueueEntity
            }
            
            orderId
        } catch (localError: Exception) {
            Log.e("OrderRepository", "❌ CRITICAL: Local vault write failed: ${localError.message}")
            throw localError // Re-throw to inform UI
        }
    }
    
    /**
     * Add item to order (CRASH-PROOF VERSION)
     * Local-first: Write to database before attempting sync
     */
    suspend fun addOrderDetail(detail: OrderDetailEntity): Long {
        return try {
            // 1. LOCAL WRITE FIRST
            val detailId = orderDetailDao.insertDetail(detail)
            
            // 2. BACKGROUND SYNC (Non-blocking)
            try {
                syncEngine.syncOrderDetailUpdate(detail.copy(detailId = detailId))
            } catch (syncError: Exception) {
                Log.e("OrderRepository", "⚠️ Detail sync failed, queued: ${syncError.message}")
            }
            
            detailId
        } catch (localError: Exception) {
            Log.e("OrderRepository", "❌ CRITICAL: Detail write failed: ${localError.message}")
            throw localError
        }
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
        
        // Broadcast claim to all tablets via mesh network
        syncEngine.syncChiefClaim(detailId, chiefId, chiefName)
    }
    
    /**
     * Mark detail as ready
     */
    suspend fun markDetailReady(detailId: Long, readyTime: Long) {
        orderDetailDao.updateReadyTime(detailId, readyTime)
        orderDetailDao.updateStatus(detailId, "READY")
        
        // Broadcast ready status to all tablets
        val detail = orderDetailDao.getDetailById(detailId)
        detail?.let {
            syncEngine.syncChiefClaim(detailId, it.claimedBy ?: 0L, it.claimedByName ?: "Unknown")
        }
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
