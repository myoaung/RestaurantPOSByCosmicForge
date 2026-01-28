package com.cosmicforge.pos.ui.waiter

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cosmicforge.pos.core.security.AuditLogger
import com.cosmicforge.pos.core.security.DeviceInfoProvider
import com.cosmicforge.pos.data.database.AppDatabase
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import com.cosmicforge.pos.data.database.entities.TableEntity
import com.cosmicforge.pos.data.repository.MenuRepository
import com.cosmicforge.pos.data.repository.OrderRepository
import com.cosmicforge.pos.data.repository.TableRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Order Finalization
 * Tests database persistence, table synchronization, and audit logging
 */
@RunWith(AndroidJUnit4::class)
class OrderFinalizationIntegrationTest {
    
    private lateinit var database: AppDatabase
    private lateinit var orderRepository: OrderRepository
    private lateinit var tableRepository: TableRepository
    private lateinit var menuRepository: MenuRepository
    private lateinit var auditLogger: AuditLogger
    private lateinit var finalizationViewModel: OrderFinalizationViewModel
    private lateinit var cartViewModel: CartViewModel
    
    // Test data
    private lateinit var testTable: TableEntity
    private lateinit var cosmicEspresso: MenuItemEntity
    private lateinit var nebulaBurger: MenuItemEntity
    
    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Create in-memory database for fast, isolated tests
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        // Initialize repositories
        orderRepository = OrderRepository(
            orderDao = database.orderDao(),
            orderDetailDao = database.orderDetailDao()
        )
        
        tableRepository = TableRepository(
            tableDao = database.tableDao()
        )
        
        menuRepository = MenuRepository(
            menuItemDao = database.menuItemDao()
        )
        
        // Initialize AuditLogger with mock DeviceInfoProvider
        val deviceInfoProvider = object : DeviceInfoProvider {
            override fun getDeviceId(): String = "TEST_DEVICE"
            override fun getIpAddress(): String = "127.0.0.1"
        }
        
        auditLogger = AuditLogger(
            securityAuditDao = database.securityAuditDao(),
            deviceInfoProvider = deviceInfoProvider
        )
        
        // Initialize ViewModels
        finalizationViewModel = OrderFinalizationViewModel(
            orderRepository = orderRepository,
            tableRepository = tableRepository,
            auditLogger = auditLogger
        )
        
        cartViewModel = CartViewModel()
        
        // Pre-populate test data
        setupTestData()
    }
    
    private suspend fun setupTestData() {
        // Create test table (Table 5)
        testTable = TableEntity(
            tableNumber = "T-05",
            capacity = 4,
            shape = "Square",
            status = "AVAILABLE",
            positionX = 100f,
            positionY = 100f
        )
        val tableId = database.tableDao().insertTable(testTable)
        testTable = testTable.copy(tableId = tableId)
        
        // Create menu items with distinct tax rates
        cosmicEspresso = MenuItemEntity(
            itemName = "Cosmic Espresso",
            itemNameMm = "ကော့စမစ် အက်စပရက်ဆို",
            category = "Drinks",
            price = 4.50,
            taxRate = 0.05, // 5% tax
            prepStation = "BAR",
            isAvailable = true
        )
        val espressoId = database.menuItemDao().insertItem(cosmicEspresso)
        cosmicEspresso = cosmicEspresso.copy(itemId = espressoId)
        
        nebulaBurger = MenuItemEntity(
            itemName = "Nebula Burger",
            itemNameMm = "နီဗျူလာ ဘာဂါ",
            category = "Mains",
            price = 12.00,
            taxRate = 0.08, // 8% tax
            prepStation = "GRILL",
            isAvailable = true
        )
        val burgerId = database.menuItemDao().insertItem(nebulaBurger)
        nebulaBurger = nebulaBurger.copy(itemId = burgerId)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    /**
     * HAPPY PATH: Test complete order finalization flow
     */
    @Test
    fun testOrderFinalization_HappyPath() = runBlocking {
        // ARRANGE: Add items to cart
        cartViewModel.addItem(cosmicEspresso) // $4.50 + 5% tax = $0.225
        cartViewModel.addItem(nebulaBurger)   // $12.00 + 8% tax = $0.96
        
        val cartState = cartViewModel.cartState.value
        
        // Expected calculations
        val expectedSubtotal = 4.50 + 12.00 // $16.50
        val expectedTax = (4.50 * 0.05) + (12.00 * 0.08) // $0.225 + $0.96 = $1.185
        val expectedGrandTotal = expectedSubtotal + expectedTax // $17.685
        
        // Verify cart calculations
        assertEquals(expectedSubtotal, cartState.subtotal, 0.001)
        assertEquals(expectedTax, cartState.tax, 0.001)
        assertEquals(expectedGrandTotal, cartState.grandTotal, 0.001)
        
        // ACT: Finalize order
        finalizationViewModel.finalizeOrder(
            tableId = testTable.tableId,
            tableName = testTable.tableNumber,
            waiterName = "Test Waiter",
            waiterId = 1L,
            cartState = cartState
        )
        
        // Wait for finalization to complete
        var finalizationState = finalizationViewModel.finalizationState.value
        var attempts = 0
        while (finalizationState is FinalizationState.Loading && attempts < 50) {
            Thread.sleep(100)
            finalizationState = finalizationViewModel.finalizationState.value
            attempts++
        }
        
        // ASSERT: Verify success state
        assertTrue(finalizationState is FinalizationState.Success, "Finalization should succeed")
        val successState = finalizationState as FinalizationState.Success
        
        // ASSERT 1: Order Records
        val orders = database.orderDao().getAllOrders().first()
        assertEquals(1, orders.size, "Exactly one OrderEntity should exist")
        
        val order = orders[0]
        assertEquals(testTable.tableId, order.tableId, "Order should be linked to test table")
        assertEquals("Test Waiter", order.waiterName, "Waiter name should match")
        assertEquals(1L, order.waiterId, "Waiter ID should match")
        assertEquals("PENDING", order.status, "Initial status should be PENDING")
        assertEquals("DINE_IN", order.orderType, "Order type should be DINE_IN")
        
        // ASSERT 2: Financial Integrity
        assertEquals(
            expectedGrandTotal,
            order.totalAmount,
            0.001,
            "Total amount should match cart grand total including dynamic tax"
        )
        
        // ASSERT 3: Order Details
        val orderDetails = database.orderDetailDao().getDetailsByOrder(order.orderId).first()
        assertEquals(2, orderDetails.size, "Exactly two OrderDetailEntity rows should exist")
        
        val espressoDetail = orderDetails.find { it.itemName == "Cosmic Espresso" }
        assertNotNull(espressoDetail, "Cosmic Espresso detail should exist")
        assertEquals(1, espressoDetail.quantity)
        assertEquals(4.50, espressoDetail.price, 0.001)
        assertEquals(4.50, espressoDetail.totalPrice, 0.001)
        assertEquals("BAR", espressoDetail.prepStation)
        
        val burgerDetail = orderDetails.find { it.itemName == "Nebula Burger" }
        assertNotNull(burgerDetail, "Nebula Burger detail should exist")
        assertEquals(1, burgerDetail.quantity)
        assertEquals(12.00, burgerDetail.price, 0.001)
        assertEquals(12.00, burgerDetail.totalPrice, 0.001)
        assertEquals("GRILL", burgerDetail.prepStation)
        
        // ASSERT 4: Table Sync
        val updatedTable = database.tableDao().getTableById(testTable.tableId)
        assertNotNull(updatedTable, "Table should still exist")
        assertEquals("OCCUPIED", updatedTable.status, "Table status should be OCCUPIED")
        assertEquals(order.orderId, updatedTable.currentOrderId, "Table should be linked to order")
        
        // ASSERT 5: Audit Trail
        val auditLogs = database.securityAuditDao().getAllAudits().first()
        assertTrue(auditLogs.isNotEmpty(), "Audit log should exist")
        
        val orderAudit = auditLogs.find { it.actionType == "ORDER_FINALIZED" }
        assertNotNull(orderAudit, "ORDER_FINALIZED audit entry should exist")
        assertEquals(1L, orderAudit.userId, "Audit should contain correct waiter ID")
        assertEquals("Test Waiter", orderAudit.userName, "Audit should contain correct waiter name")
        assertEquals(3, orderAudit.roleLevel, "Audit should have ROLE_WAITER level")
        assertEquals("Order", orderAudit.targetType, "Audit target type should be Order")
        assertEquals(order.orderId.toString(), orderAudit.targetId, "Audit should reference order ID")
        assertTrue(orderAudit.wasSuccessful, "Audit should mark action as successful")
        assertTrue(
            orderAudit.actionDetails?.contains("finalized") == true,
            "Audit details should mention finalization"
        )
    }
    
    /**
     * Test multiple items with same tax rate
     */
    @Test
    fun testOrderFinalization_MultipleQuantities() = runBlocking {
        // ARRANGE: Add multiple of same item
        cartViewModel.addItem(cosmicEspresso)
        cartViewModel.addItem(cosmicEspresso) // 2 total
        
        val cartState = cartViewModel.cartState.value
        
        // Expected: 2 × $4.50 = $9.00, tax = $9.00 × 5% = $0.45
        assertEquals(9.00, cartState.subtotal, 0.001)
        assertEquals(0.45, cartState.tax, 0.001)
        assertEquals(9.45, cartState.grandTotal, 0.001)
        
        // ACT: Finalize
        finalizationViewModel.finalizeOrder(
            tableId = testTable.tableId,
            tableName = testTable.tableNumber,
            waiterName = "Test Waiter",
            waiterId = 1L,
            cartState = cartState
        )
        
        // Wait for completion
        Thread.sleep(500)
        
        // ASSERT: Verify order detail quantity
        val orders = database.orderDao().getAllOrders().first()
        val orderDetails = database.orderDetailDao().getDetailsByOrder(orders[0].orderId).first()
        
        assertEquals(1, orderDetails.size, "Should have one detail entry")
        assertEquals(2, orderDetails[0].quantity, "Quantity should be 2")
        assertEquals(9.00, orderDetails[0].totalPrice, 0.001, "Total price should be $9.00")
    }
    
    /**
     * FAILURE SCENARIO: Test database constraint violation
     * Verify transaction rollback keeps table AVAILABLE
     */
    @Test
    fun testOrderFinalization_DatabaseFailure_RollbackTableStatus() = runBlocking {
        // ARRANGE: Add items to cart
        cartViewModel.addItem(cosmicEspresso)
        val cartState = cartViewModel.cartState.value
        
        // Verify table is AVAILABLE before
        val tableBefore = database.tableDao().getTableById(testTable.tableId)
        assertEquals("AVAILABLE", tableBefore?.status)
        
        // ACT: Attempt to finalize with invalid tableId (simulate constraint violation)
        finalizationViewModel.finalizeOrder(
            tableId = 99999L, // Non-existent table ID
            tableName = "Invalid Table",
            waiterName = "Test Waiter",
            waiterId = 1L,
            cartState = cartState
        )
        
        // Wait for completion
        Thread.sleep(500)
        
        // ASSERT: Verify error state
        val finalizationState = finalizationViewModel.finalizationState.value
        assertTrue(
            finalizationState is FinalizationState.Error,
            "Finalization should fail with error state"
        )
        
        // ASSERT: Verify no order was created
        val orders = database.orderDao().getAllOrders().first()
        assertEquals(0, orders.size, "No order should be created on failure")
        
        // ASSERT: Verify original table status unchanged (rollback)
        val tableAfter = database.tableDao().getTableById(testTable.tableId)
        assertEquals(
            "AVAILABLE",
            tableAfter?.status,
            "Table status should remain AVAILABLE after failed transaction"
        )
        assertNotNull(tableAfter)
        assertEquals(null, tableAfter.currentOrderId, "Table should not be linked to any order")
    }
    
    /**
     * Test empty cart validation
     */
    @Test
    fun testOrderFinalization_EmptyCart_ReturnsError() = runBlocking {
        // ARRANGE: Empty cart
        val emptyCartState = cartViewModel.cartState.value
        assertEquals(0, emptyCartState.items.size)
        
        // ACT: Attempt to finalize
        finalizationViewModel.finalizeOrder(
            tableId = testTable.tableId,
            tableName = testTable.tableNumber,
            waiterName = "Test Waiter",
            waiterId = 1L,
            cartState = emptyCartState
        )
        
        // ASSERT: Should return error immediately
        val finalizationState = finalizationViewModel.finalizationState.value
        assertTrue(finalizationState is FinalizationState.Error)
        assertEquals("Cart is empty", (finalizationState as FinalizationState.Error).message)
        
        // Verify no database changes
        val orders = database.orderDao().getAllOrders().first()
        assertEquals(0, orders.size)
        
        val table = database.tableDao().getTableById(testTable.tableId)
        assertEquals("AVAILABLE", table?.status)
    }
    
    /**
     * Test parcel order (no table)
     */
    @Test
    fun testOrderFinalization_ParcelOrder_NoTableUpdate() = runBlocking {
        // ARRANGE: Add items
        cartViewModel.addItem(cosmicEspresso)
        val cartState = cartViewModel.cartState.value
        
        // ACT: Finalize as parcel (tableId = null)
        finalizationViewModel.finalizeOrder(
            tableId = null,
            tableName = null,
            waiterName = "Test Waiter",
            waiterId = 1L,
            cartState = cartState
        )
        
        // Wait for completion
        Thread.sleep(500)
        
        // ASSERT: Order created
        val orders = database.orderDao().getAllOrders().first()
        assertEquals(1, orders.size)
        assertEquals("PARCEL", orders[0].orderType)
        assertEquals(null, orders[0].tableId)
        
        // ASSERT: Test table unchanged
        val table = database.tableDao().getTableById(testTable.tableId)
        assertEquals("AVAILABLE", table?.status, "Table should remain AVAILABLE for parcel orders")
    }
}
