package com.cosmicforge.pos.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import com.cosmicforge.pos.data.database.entities.OrderDetailEntity
import com.cosmicforge.pos.data.database.entities.OrderEntity
import com.cosmicforge.pos.data.database.entities.TableEntity
import com.cosmicforge.pos.data.repository.MenuRepository
import com.cosmicforge.pos.data.repository.OrderRepository
import com.cosmicforge.pos.data.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for order entry with parcel fee auto-calculation
 */
@HiltViewModel
class OrderEntryViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository
) : ViewModel() {
    
    private val _selectedTable = MutableStateFlow<TableEntity?>(null)
    val selectedTable: StateFlow<TableEntity?> = _selectedTable.asStateFlow()
    
    private val _isParcelOrder = MutableStateFlow(false)
    val isParcelOrder: StateFlow<Boolean> = _isParcelOrder.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    private val _currentOrder = MutableStateFlow<OrderEntity?>(null)
    val currentOrder: StateFlow<OrderEntity?> = _currentOrder.asStateFlow()
    
    private val _orderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val orderItems: StateFlow<List<OrderItem>> = _orderItems.asStateFlow()
    
    val categories: StateFlow<List<String>> = menuRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val menuItems: StateFlow<List<MenuItemEntity>> = selectedCategory
        .flatMapLatest { category ->
            category?.let {
                menuRepository.getMenuItemsByCategory(it)
            } ?: menuRepository.getAvailableMenuItems()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val orderSummary: StateFlow<OrderSummary> = combine(
        _orderItems,
        _isParcelOrder,
        _customParcelFee
    ) { items, isParcel, customFee ->
        val subtotal = items.sumOf { it.totalPrice }
        val parcelFee = if (isParcel) {
            customFee ?: PARCEL_FEE // Use custom or default
        } else {
            0.0
        }
        val total = subtotal + parcelFee
        
        OrderSummary(
            subtotal = subtotal,
            parcelFee = parcelFee,
            total = total,
            itemCount = items.sumOf { it.quantity },
            isCustomParcelFee = customFee != null && isParcel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrderSummary()
    )
    
    /**
     * Set table for order
     */
    fun setTable(table: TableEntity) {
        _selectedTable.value = table
        _isParcelOrder.value = false // Table orders are dine-in
    }
    
    /**
     * Toggle parcel mode
     * Auto-applies packaging fee
     */
    fun toggleParcelMode(isParcel: Boolean) {
        _isParcelOrder.value = isParcel
        if (isParcel) {
            _selectedTable.value = null // Parcel orders don't need tables
        }
    }
    
    /**
     * Set custom parcel fee (manual override)
     * Defaults to PARCEL_FEE if not set
     */
    fun setCustomParcelFee(customFee: Double?) {
        _customParcelFee.value = customFee
    }
    
    private val _customParcelFee = MutableStateFlow<Double?>(null)
    val customParcelFee: StateFlow<Double?> = _customParcelFee.asStateFlow()
    
    /**
     * Select category
     */
    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
    
    /**
     * Add item to order
     */
    fun addItem(menuItem: MenuItemEntity, quantity: Int = 1) {
        val existingItem = _orderItems.value.find { it.menuItem.itemId == menuItem.itemId }
        
        _orderItems.value = if (existingItem != null) {
            _orderItems.value.map {
                if (it.menuItem.itemId == menuItem.itemId) {
                    it.copy(quantity = it.quantity + quantity)
                } else {
                    it
                }
            }
        } else {
            _orderItems.value + OrderItem(menuItem, quantity)
        }
    }
    
    /**
     * Update item quantity
     */
    fun updateItemQuantity(menuItemId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(menuItemId)
            return
        }
        
        _orderItems.value = _orderItems.value.map {
            if (it.menuItem.itemId == menuItemId) {
                it.copy(quantity = newQuantity)
            } else {
                it
            }
        }
    }
    
    /**
     * Remove item from order
     */
    fun removeItem(menuItemId: Long) {
        _orderItems.value = _orderItems.value.filter { it.menuItem.itemId != menuItemId }
    }
    
    /**
     * Clear order
     */
    fun clearOrder() {
        _orderItems.value = emptyList()
        _currentOrder.value = null
    }
    
    /**
     * Submit order
     */
    fun submitOrder(
        waiterName: String,
        waiterId: Long,
        customerName: String? = null,
        onSuccess: (Long) -> Unit
    ) {
        if (_orderItems.value.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val summary = orderSummary.value
                
                // Create order entity
                val order = OrderEntity(
                    orderNumber = generateOrderNumber(),
                    tableId = _selectedTable.value?.tableId,
                    orderType = if (_isParcelOrder.value) "PARCEL" else "DINE_IN",
                    customerName = customerName,
                    waiterName = waiterName,
                    waiterId = waiterId,
                    subtotal = summary.subtotal,
                    parcelFee = summary.parcelFee,
                    customParcelFee = _customParcelFee.value,
                    parcelFeeOverrideBy = if (_customParcelFee.value != null) waiterName else null,
                    totalAmount = summary.total,
                    status = "PENDING",
                    syncId = UUID.randomUUID().toString(),
                    deviceId = "DEVICE_${System.currentTimeMillis()}"
                )
                
                val orderId = orderRepository.createOrder(order)
                
                // Add order details
                _orderItems.value.forEach { item ->
                    val detail = OrderDetailEntity(
                        orderId = orderId,
                        itemId = item.menuItem.itemId,
                        itemName = item.menuItem.itemName,
                        itemNameMm = item.menuItem.itemNameMm,
                        quantity = item.quantity,
                        price = item.menuItem.price,
                        totalPrice = item.totalPrice,
                        prepStation = item.menuItem.prepStation,
                        status = "PENDING"
                    )
                    
                    orderRepository.addOrderDetail(detail)
                }
                
                // Update table status if dine-in
                _selectedTable.value?.let { table ->
                    tableRepository.assignOrderToTable(table.tableId, orderId)
                }
                
                onSuccess(orderId)
                clearOrder()
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        return "ORD${timestamp.toString().takeLast(8)}"
    }
    
    companion object {
        private const val PARCEL_FEE = 1000.0 // MMK
    }
}

/**
 * Order item with quantity
 */
data class OrderItem(
    val menuItem: MenuItemEntity,
    val quantity: Int,
    val modifiers: String? = null,
    val specialInstructions: String? = null
) {
    val totalPrice: Double
        get() = menuItem.price * quantity
}

/**
 * Order summary with parcel fee
 */
data class OrderSummary(
    val subtotal: Double = 0.0,
    val parcelFee: Double = 0.0,
    val total: Double = 0.0,
    val itemCount: Int = 0,
    val isCustomParcelFee: Boolean = false
)
