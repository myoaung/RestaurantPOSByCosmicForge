package com.cosmicforge.pos.ui.waiter

import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for CartViewModel
 * Verifies tax calculations match Phase 3 mock data rates:
 * - Drinks: 5% tax
 * - Mains/Sides/Desserts: 8% tax
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    
    private lateinit var viewModel: CartViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Mock menu items from Phase 3 seeder
    private val cosmicEspresso = MenuItemEntity(
        itemId = 1L,
        itemName = "Cosmic Espresso",
        itemNameMm = "ကော့စမစ် အက်စပရက်ဆို",
        category = "Drinks",
        price = 4.50,
        taxRate = 0.05, // 5%
        prepStation = "BAR",
        isAvailable = true
    )
    
    private val nebulaBurger = MenuItemEntity(
        itemId = 2L,
        itemName = "Nebula Burger",
        itemNameMm = "နီဗျူလာ ဘာဂါ",
        category = "Mains",
        price = 12.00,
        taxRate = 0.08, // 8%
        prepStation = "GRILL",
        isAvailable = true
    )
    
    private val starryFries = MenuItemEntity(
        itemId = 3L,
        itemName = "Starry Fries",
        itemNameMm = "ကြယ်စင်း ကြော်",
        category = "Sides",
        price = 5.50,
        taxRate = 0.08, // 8%
        prepStation = "FRY",
        isAvailable = true
    )
    
    private val blackHoleCake = MenuItemEntity(
        itemId = 4L,
        itemName = "Black Hole Cake",
        itemNameMm = "တွင်းနက် ကိတ်မုန့်",
        category = "Desserts",
        price = 7.00,
        taxRate = 0.08, // 8%
        prepStation = "PASTRY",
        isAvailable = false
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CartViewModel()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `adding item updates cart state`() {
        // When
        viewModel.addItem(cosmicEspresso)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(cosmicEspresso.itemId, state.items[0].menuItem.itemId)
        assertEquals(1, state.items[0].quantity)
    }
    
    @Test
    fun `adding same item twice increments quantity`() {
        // When
        viewModel.addItem(cosmicEspresso)
        viewModel.addItem(cosmicEspresso)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(2, state.items[0].quantity)
    }
    
    @Test
    fun `subtotal calculation is correct`() {
        // When
        viewModel.addItem(cosmicEspresso) // $4.50
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(4.50, state.subtotal, 0.001)
    }
    
    @Test
    fun `tax calculation for drinks is 5 percent`() {
        // When
        viewModel.addItem(cosmicEspresso) // $4.50 × 5% = $0.225
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0.225, state.tax, 0.001)
    }
    
    @Test
    fun `tax calculation for mains is 8 percent`() {
        // When
        viewModel.addItem(nebulaBurger) // $12.00 × 8% = $0.96
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0.96, state.tax, 0.001)
    }
    
    @Test
    fun `tax calculation for sides is 8 percent`() {
        // When
        viewModel.addItem(starryFries) // $5.50 × 8% = $0.44
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0.44, state.tax, 0.001)
    }
    
    @Test
    fun `grand total includes subtotal plus tax`() {
        // When
        viewModel.addItem(cosmicEspresso) // $4.50 + $0.225 tax
        viewModel.addItem(nebulaBurger)   // $12.00 + $0.96 tax
        
        // Then
        val state = viewModel.cartState.value
        // Subtotal: $4.50 + $12.00 = $16.50
        assertEquals(16.50, state.subtotal, 0.001)
        // Tax: $0.225 + $0.96 = $1.185
        assertEquals(1.185, state.tax, 0.001)
        // Grand Total: $16.50 + $1.185 = $17.685
        assertEquals(17.685, state.grandTotal, 0.001)
    }
    
    @Test
    fun `multiple quantities calculate tax correctly`() {
        // When
        viewModel.addItem(cosmicEspresso)
        viewModel.addItem(cosmicEspresso) // 2 × $4.50 = $9.00
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(9.00, state.subtotal, 0.001)
        // Tax: $9.00 × 5% = $0.45
        assertEquals(0.45, state.tax, 0.001)
        assertEquals(9.45, state.grandTotal, 0.001)
    }
    
    @Test
    fun `mixed items calculate combined tax correctly`() {
        // When
        viewModel.addItem(cosmicEspresso) // $4.50 × 5% = $0.225
        viewModel.addItem(starryFries)    // $5.50 × 8% = $0.44
        viewModel.addItem(blackHoleCake)  // $7.00 × 8% = $0.56
        
        // Then
        val state = viewModel.cartState.value
        // Subtotal: $4.50 + $5.50 + $7.00 = $17.00
        assertEquals(17.00, state.subtotal, 0.001)
        // Tax: $0.225 + $0.44 + $0.56 = $1.225
        assertEquals(1.225, state.tax, 0.001)
        // Grand Total: $17.00 + $1.225 = $18.225
        assertEquals(18.225, state.grandTotal, 0.001)
    }
    
    @Test
    fun `removing item updates totals`() {
        // Given
        viewModel.addItem(cosmicEspresso)
        viewModel.addItem(nebulaBurger)
        
        // When
        viewModel.removeItem(cosmicEspresso.itemId)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(1, state.items.size)
        assertEquals(12.00, state.subtotal, 0.001)
        assertEquals(0.96, state.tax, 0.001)
        assertEquals(12.96, state.grandTotal, 0.001)
    }
    
    @Test
    fun `updating quantity to zero removes item`() {
        // Given
        viewModel.addItem(cosmicEspresso)
        
        // When
        viewModel.updateQuantity(cosmicEspresso.itemId, 0)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0, state.items.size)
        assertEquals(0.0, state.subtotal, 0.001)
        assertEquals(0.0, state.tax, 0.001)
        assertEquals(0.0, state.grandTotal, 0.001)
    }
    
    @Test
    fun `updating quantity recalculates totals`() {
        // Given
        viewModel.addItem(nebulaBurger)
        
        // When
        viewModel.updateQuantity(nebulaBurger.itemId, 3)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(3, state.items[0].quantity)
        // Subtotal: 3 × $12.00 = $36.00
        assertEquals(36.00, state.subtotal, 0.001)
        // Tax: $36.00 × 8% = $2.88
        assertEquals(2.88, state.tax, 0.001)
        // Grand Total: $36.00 + $2.88 = $38.88
        assertEquals(38.88, state.grandTotal, 0.001)
    }
    
    @Test
    fun `clearing cart resets all values`() {
        // Given
        viewModel.addItem(cosmicEspresso)
        viewModel.addItem(nebulaBurger)
        
        // When
        viewModel.clearCart()
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0, state.items.size)
        assertEquals(0.0, state.subtotal, 0.001)
        assertEquals(0.0, state.tax, 0.001)
        assertEquals(0.0, state.grandTotal, 0.001)
    }
    
    @Test
    fun `empty cart has zero totals`() {
        // When (no items added)
        
        // Then
        val state = viewModel.cartState.value
        assertEquals(0, state.items.size)
        assertEquals(0.0, state.subtotal, 0.001)
        assertEquals(0.0, state.tax, 0.001)
        assertEquals(0.0, state.grandTotal, 0.001)
    }
}
