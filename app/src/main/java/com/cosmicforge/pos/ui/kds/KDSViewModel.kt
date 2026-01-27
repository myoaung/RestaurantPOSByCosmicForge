package com.cosmicforge.pos.ui.kds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.pos.core.security.AuditLogger
import com.cosmicforge.pos.data.database.entities.OrderDetailEntity
import com.cosmicforge.pos.data.database.entities.OrderEntity
import com.cosmicforge.pos.data.database.entities.UserEntity
import com.cosmicforge.pos.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Kitchen Display System with chief accountability
 */
@HiltViewModel
class KDSViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val auditLogger: AuditLogger
) : ViewModel() {
    
    private val _selectedStation = MutableStateFlow<String?>(null)
    val selectedStation: StateFlow<String?> = _selectedStation.asStateFlow()
    
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()
    
    // All pending orders
    val pendingOrders: StateFlow<List<KDSTicket>> = orderRepository
        .getOrdersByStatus("PENDING")
        .combine(orderRepository.getOrdersByStatus("COOKING")) { pending, cooking ->
            (pending + cooking).map { order ->
                loadTicket(order)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Filtered orders by prep station
    val filteredOrders: StateFlow<List<KDSTicket>> = combine(
        pendingOrders,
        _selectedStation
    ) { orders, station ->
        station?.let { s ->
            orders.map { ticket ->
                ticket.copy(
                    details = ticket.details.filter { it.prepStation == s }
                )
            }.filter { it.details.isNotEmpty() }
        } ?: orders
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Set current chief user
     */
    fun setCurrentUser(user: UserEntity) {
        _currentUser.value = user
    }
    
    /**
     * Select prep station filter
     */
    fun selectStation(station: String?) {
        _selectedStation.value = station
    }
    
    /**
     * Claim order detail for cooking
     * Implements 8-chief accountability tracking
     */
    fun claimOrderDetail(
        orderId: Long,
        detailId: Long,
        chiefId: Long,
        chiefName: String
    ) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            
            // Update order detail with claim info
            orderRepository.claimOrderDetail(
                detailId = detailId,
                chiefId = chiefId,
                chiefName = chiefName,
                claimTime = currentTime
            )
            
            // Log claim action
            auditLogger.logChiefClaim(
                chiefId = chiefId,
                chiefName = chiefName,
                orderId = orderId,
                detailId = detailId
            )
        }
    }
    
    /**
     * Mark order detail as ready
     * Calculates performance timer (claim → ready)
     */
    fun markDetailReady(
        detailId: Long,
        chiefId: Long,
        chiefName: String
    ) {
        viewModelScope.launch {
            val readyTime = System.currentTimeMillis()
            
            // Get the detail to calculate cook time
            val detail = orderRepository.getOrderDetailById(detailId)
            detail?.let {
                val cookTime = if (it.claimedAtTimestamp != null) {
                    readyTime - it.claimedAtTimestamp
                } else {
                    0L
                }
                
                // Update status
                orderRepository.markDetailReady(detailId, readyTime)
                
                // Log performance
                auditLogger.logChiefPerformance(
                    chiefId = chiefId,
                    chiefName = chiefName,
                    detailId = detailId,
                    cookTimeMs = cookTime
                )
            }
        }
    }
    
    /**
     * Load complete ticket data
     */
    private suspend fun loadTicket(order: OrderEntity): KDSTicket {
        val details = orderRepository.getOrderDetailsSnapshot(order.orderId)
        return KDSTicket(
            order = order,
            details = details,
            elapsedTime = System.currentTimeMillis() - order.createdAt
        )
    }
}

/**
 * KDS ticket with order and details
 */
data class KDSTicket(
    val order: OrderEntity,
    val details: List<OrderDetailEntity>,
    val elapsedTime: Long
) {
    val totalItems: Int
        get() = details.sumOf { it.quantity }
    
    val claimedCount: Int
        get() = details.count { it.claimedBy != null }
    
    val readyCount: Int
        get() = details.count { it.status == "READY" }
    
    val pendingCount: Int
        get() = details.count { it.status == "PENDING" }
}
