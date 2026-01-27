package com.cosmicforge.pos.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import com.cosmicforge.pos.data.database.entities.TableEntity

/**
 * Order entry screen with category-based menu and parcel toggle
 */
@Composable
fun OrderEntryScreen(
    table: TableEntity?,
    waiterName: String,
    waiterId: Long,
    onOrderSubmitted: () -> Unit,
    onBack: () -> Unit,
    viewModel: OrderEntryViewModel = hiltViewModel()
) {
    LaunchedEffect(table) {
        table?.let { viewModel.setTable(it) }
    }
    
    val isParcelOrder by viewModel.isParcelOrder.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val orderItems by viewModel.orderItems.collectAsState()
    val orderSummary by viewModel.orderSummary.collectAsState()
    
    var showCheckoutDialog by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Menu selection
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            OrderEntryHeader(
                table = table,
                isParcelOrder = isParcelOrder,
                onParcelToggle = { viewModel.toggleParcelMode(it) },
                onBack = onBack
            )
            
            Divider()
            
            // Category filter
            CategoryFilter(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )
            
            Divider()
            
            // Menu grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemCard(
                        item = item,
                        onAddClick = { viewModel.addItem(item) }
                    )
                }
            }
        }
        
        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        // Right: Order cart
        OrderCart(
            orderItems = orderItems,
            orderSummary = orderSummary,
            isParcelOrder = isParcelOrder,
            onQuantityChange = { itemId, qty -> viewModel.updateItemQuantity(itemId, qty) },
            onRemoveItem = { viewModel.removeItem(it) },
            onCheckout = { showCheckoutDialog = true }
        )
    }
    
    // Checkout dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            orderSummary = orderSummary,
            isParcelOrder = isParcelOrder,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { customerName ->
                viewModel.submitOrder(
                    waiterName = waiterName,
                    waiterId = waiterId,
                    customerName = customerName
                ) {
                    showCheckoutDialog = false
                    onOrderSubmitted()
                }
            }
        )
    }
}

@Composable
private fun OrderEntryHeader(
    table: TableEntity?,
    isParcelOrder: Boolean,
    onParcelToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isParcelOrder) "Parcel Order" else table?.tableName ?: "New Order",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Parcel toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Parcel",
                    tint = if (isParcelOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = isParcelOrder,
                    onCheckedChange = onParcelToggle,
                    enabled = table == null
                )
                Text(
                    text = "Parcel (+1000 MMK)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isParcelOrder) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItemEntity,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAddClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            item.itemNameMm?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${item.price.toInt()} MMK",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            if (!item.isAvailable) {
                Text(
                    text = "Out of Stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun OrderCart(
    orderItems: List<OrderItem>,
    orderSummary: OrderSummary,
    isParcelOrder: Boolean,
    onQuantityChange: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(400.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Cart header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Badge {
                    Text(orderSummary.itemCount.toString())
                }
            }
        }
        
        // Cart items
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orderItems) { orderItem ->
                OrderItemRow(
                    orderItem = orderItem,
                    onQuantityChange = onQuantityChange,
                    onRemove = onRemoveItem
                )
            }
        }
        
        Divider()
        
        // Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${orderSummary.subtotal.toInt()} MMK",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (isParcelOrder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Parcel Fee:", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${orderSummary.parcelFee.toInt()} MMK",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${orderSummary.total.toInt()} MMK",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = orderItems.isNotEmpty()
            ) {
                Icon(Icons.Default.Payment, "Checkout")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proceed to Payment")
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    orderItem: OrderItem,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = orderItem.menuItem.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${orderItem.menuItem.price.toInt()} MMK each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Quantity controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onQuantityChange(orderItem.menuItem.itemId, orderItem.quantity - 1) }
                ) {
                    Icon(Icons.Default.Remove, "Decrease")
                }
                
                Text(
                    text = orderItem.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { onQuantityChange(orderItem.menuItem.itemId, orderItem.quantity + 1) }
                ) {
                    Icon(Icons.Default.Add, "Increase")
                }
                
                IconButton(
                    onClick = { onRemove(orderItem.menuItem.itemId) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, "Remove")
                }
            }
        }
    }
}

@Composable
private fun CheckoutDialog(
    orderSummary: OrderSummary,
    isParcelOrder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Order") },
        text = {
            Column {
                Text("Total: ${orderSummary.total.toInt()} MMK")
                
                if (isParcelOrder) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(customerName.ifBlank { null }) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
