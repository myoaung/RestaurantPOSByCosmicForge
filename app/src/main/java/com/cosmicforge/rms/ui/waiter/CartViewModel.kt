package com.cosmicforge.rms.ui.waiter

import androidx.lifecycle.ViewModel
import com.cosmicforge.rms.data.database.entities.MenuItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for managing shopping cart state and calculations
 * Phase 4: Transaction Engine
 */
@HiltViewModel
class CartViewModel @Inject constructor() : ViewModel() {
    
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()
    
    /**
     * Add item to cart or increment quantity if already exists
     */
    fun addItem(menuItem: MenuItemEntity) {
        val currentItems = _cartState.value.items.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst { it.menuItem.itemId == menuItem.itemId }
        
        if (existingItemIndex >= 0) {
            // Item already in cart, increment quantity
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            // New item, add to cart
            currentItems.add(CartItem(menuItem = menuItem, quantity = 1))
        }
        
        updateCartState(currentItems)
    }
    
    /**
     * Remove item from cart completely
     */
    fun removeItem(itemId: Long) {
        val currentItems = _cartState.value.items.toMutableList()
        currentItems.removeAll { it.menuItem.itemId == itemId }
        updateCartState(currentItems)
    }
    
    /**
     * Update quantity for a specific item
     */
    fun updateQuantity(itemId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeItem(itemId)
            return
        }
        
        val currentItems = _cartState.value.items.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.menuItem.itemId == itemId }
        
        if (itemIndex >= 0) {
            currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = quantity)
            updateCartState(currentItems)
        }
    }
    
    /**
     * Clear all items from cart
     */
    fun clearCart() {
        _cartState.value = CartState()
    }
    
    /**
     * Update cart state and recalculate totals
     */
    private fun updateCartState(items: List<CartItem>) {
        val subtotal = calculateSubtotal(items)
        val tax = calculateTax(items)
        val grandTotal = subtotal + tax
        
        _cartState.value = CartState(
            items = items,
            subtotal = subtotal,
            tax = tax,
            grandTotal = grandTotal
        )
    }
    
    /**
     * Calculate subtotal (sum of item prices × quantities)
     */
    private fun calculateSubtotal(items: List<CartItem>): Double {
        return items.sumOf { it.menuItem.price * it.quantity }
    }
    
    /**
     * Calculate tax based on item category
     * - Drinks: 5% tax rate
     * - Others (Mains, Sides, Desserts): 8% tax rate
     */
    private fun calculateTax(items: List<CartItem>): Double {
        return items.sumOf { cartItem ->
            val itemSubtotal = cartItem.menuItem.price * cartItem.quantity
            val taxRate = cartItem.menuItem.taxRate
            itemSubtotal * taxRate
        }
    }
}

/**
 * Data class representing an item in the cart
 */
data class CartItem(
    val menuItem: MenuItemEntity,
    val quantity: Int
)

/**
 * Data class representing the complete cart state
 */
data class CartState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0
)
