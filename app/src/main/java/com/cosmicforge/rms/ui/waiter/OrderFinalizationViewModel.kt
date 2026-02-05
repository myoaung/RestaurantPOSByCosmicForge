package com.cosmicforge.rms.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.rms.core.security.AuditLogger
import com.cosmicforge.rms.data.database.entities.OrderDetailEntity
import com.cosmicforge.rms.data.database.entities.OrderEntity
import com.cosmicforge.rms.data.repository.OrderRepository
import com.cosmicforge.rms.data.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for order finalization with database persistence
 * Phase 4: Transaction Engine
 */
@HiltViewModel
class OrderFinalizationViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository,
    private val auditLogger: AuditLogger
) : ViewModel() {
    
    private val _finalizationState = MutableStateFlow<FinalizationState>(FinalizationState.Idle)
    val finalizationState: StateFlow<FinalizationState> = _finalizationState.asStateFlow()
    
    /**
     * Finalize order: Save to database, update table status, log audit
     */
    fun finalizeOrder(
        tableId: String?,
        tableName: String?,
        waiterName: String,
        waiterId: Long,
        cartState: CartState
    ) {
        if (cartState.items.isEmpty()) {
            _finalizationState.value = FinalizationState.Error("Cart is empty")
            return
        }
        
        viewModelScope.launch {
            try {
                _finalizationState.value = FinalizationState.Loading
                
                // 1. Create OrderEntity
                val orderEntity = OrderEntity(
                    orderNumber = generateOrderNumber(),
                    tableId = tableId,
                    waiterName = waiterName,
                    waiterId = waiterId,
                    customerName = null, // Can be added for parcel orders
                    totalAmount = cartState.grandTotal,
                    status = "PENDING",
                    orderType = if (tableId == null) "PARCEL" else "DINE_IN",
                    createdAt = System.currentTimeMillis(),
                    syncId = UUID.randomUUID().toString(),
                    deviceId = "DEVICE_${System.currentTimeMillis()}" // TODO: Get actual device ID
                )
                
                // Save order and get generated ID
                val orderId = orderRepository.createOrder(orderEntity)
                
                // 2. Create OrderDetailEntity records for each cart item
                cartState.items.forEach { cartItem ->
                    val orderDetail = OrderDetailEntity(
                        orderId = orderId,
                        itemId = cartItem.menuItem.itemId,
                        itemName = cartItem.menuItem.itemName,
                        itemNameMm = cartItem.menuItem.itemNameMm ?: "",
                        quantity = cartItem.quantity,
                        price = cartItem.menuItem.price,
                        unitPrice = cartItem.menuItem.price,
                        totalPrice = cartItem.menuItem.price * cartItem.quantity,
                        subtotal = cartItem.menuItem.price * cartItem.quantity,
                        prepStation = cartItem.menuItem.prepStation,
                        status = "PENDING"
                    )
                    
                    orderRepository.addOrderDetail(orderDetail)
                }
                
                // 3. Update table status to OCCUPIED (if dine-in)
                tableId?.let {
                    tableRepository.updateTableStatus(it, "OCCUPIED")
                    tableRepository.assignOrderToTable(it, orderId)
                }
                
                // 4. Log via AuditLogger
                auditLogger.logAction(
                    actionType = "ORDER_FINALIZED",
                    userId = waiterId,
                    userName = waiterName,
                    roleLevel = 3, // ROLE_WAITER
                    targetType = "Order",
                    targetId = orderId.toString(),
                    actionDetails = buildString {
                        append("Order #${orderEntity.orderNumber} finalized. ")
                        tableName?.let { append("Table: $it. ") }
                        append("Items: ${cartState.items.size}. ")
                        append("Total: ${cartState.grandTotal}")
                    },
                    wasSuccessful = true
                )
                
                // OPTIMISTIC UI: Emit success IMMEDIATELY after database write
                // Don't wait for network sync - background sync happens asynchronously
                _finalizationState.value = FinalizationState.Success(
                    orderId = orderId,
                    orderNumber = orderEntity.orderNumber,
                    tableName = tableName
                )
                
                // Background sync logging (non-blocking)
                android.util.Log.d("OrderFinalization", "✅ Order saved locally: ${orderEntity.orderNumber}")
                android.util.Log.d("OrderFinalization", "🔄 Background sync queued (non-blocking)")
                
            } catch (e: Exception) {
                _finalizationState.value = FinalizationState.Error(
                    e.message ?: "Failed to finalize order"
                )
            }
        }
    }
    
    /**
     * Reset finalization state
     */
    fun resetState() {
        _finalizationState.value = FinalizationState.Idle
    }
    
    /**
     * Generate unique order number
     */
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "ORD-$timestamp-$random"
    }
}

/**
 * Finalization state sealed class
 */
sealed class FinalizationState {
    object Idle : FinalizationState()
    object Loading : FinalizationState()
    data class Success(
        val orderId: Long,
        val orderNumber: String,
        val tableName: String?
    ) : FinalizationState()
    data class Error(val message: String) : FinalizationState()
}
