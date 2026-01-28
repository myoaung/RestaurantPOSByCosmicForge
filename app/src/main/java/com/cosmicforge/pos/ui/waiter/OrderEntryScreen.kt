package com.cosmicforge.pos.ui.waiter

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.pos.data.database.entities.MenuItemEntity
import com.cosmicforge.pos.data.database.entities.UserEntity
import java.text.NumberFormat
import java.util.Locale

/**
 * Order entry screen with two-pane layout
 * Phase 4: Transaction Engine with Database Persistence
 * Left: Cart with totals | Right: Category-filtered menu grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEntryScreen(
    tableId: Long? = null,
    tableName: String? = null,
    currentUser: UserEntity,
    onOrderFinalized: () -> Unit = {},
    onBack: () -> Unit = {},
    cartViewModel: CartViewModel = hiltViewModel(),
    menuViewModel: OrderEntryViewModel = hiltViewModel(),
    finalizationViewModel: OrderFinalizationViewModel = hiltViewModel()
) {
    val cartState by cartViewModel.cartState.collectAsState()
    val categories by menuViewModel.categories.collectAsState()
    val menuItems by menuViewModel.menuItems.collectAsState()
    val selectedCategory by menuViewModel.selectedCategory.collectAsState()
    val finalizationState by finalizationViewModel.finalizationState.collectAsState()
    
    var showFinalizeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle finalization state changes
    LaunchedEffect(finalizationState) {
        when (val state = finalizationState) {
            is FinalizationState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Order ${state.orderNumber} finalized successfully!",
                    duration = SnackbarDuration.Short
                )
                cartViewModel.clearCart()
                finalizationViewModel.resetState()
                onOrderFinalized()
            }
            is FinalizationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                finalizationViewModel.resetState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT PANE: Cart (40%)
                CartPane(
                    cartState = cartState,
                    tableName = tableName,
                    onQuantityChange = { itemId, newQty ->
                        cartViewModel.updateQuantity(itemId, newQty)
                    },
                    onRemoveItem = { itemId ->
                        cartViewModel.removeItem(itemId)
                    },
                    onClearCart = {
                        cartViewModel.clearCart()
                    },
                    onFinalizeOrder = {
                        showFinalizeDialog = true
                    },
                    onBack = onBack,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
                
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
                
                // RIGHT PANE: Menu Grid (60%)
                MenuPane(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    menuItems = menuItems,
                    onCategorySelected = { category ->
                        menuViewModel.selectCategory(category)
                    },
                    onItemSelected = { menuItem ->
                        cartViewModel.addItem(menuItem)
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            }
            
            // Loading overlay
            if (finalizationState is FinalizationState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Finalizing order...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Finalize Order Dialog
    if (showFinalizeDialog) {
        FinalizeOrderDialog(
            cartState = cartState,
            tableName = tableName,
            onDismiss = { showFinalizeDialog = false },
            onConfirm = {
                finalizationViewModel.finalizeOrder(
                    tableId = tableId,
                    tableName = tableName,
                    waiterName = currentUser.userName,
                    waiterId = currentUser.userId,
                    cartState = cartState
                )
                showFinalizeDialog = false
            }
        )
    }
}

@Composable
private fun CartPane(
    cartState: CartState,
    tableName: String?,
    onQuantityChange: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onFinalizeOrder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Surface(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        tableName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                if (cartState.items.isNotEmpty()) {
                    TextButton(onClick = onClearCart) {
                        Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }
        
        Divider()
        
        // Cart Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cartState.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Cart is empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(cartState.items) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onQuantityChange = onQuantityChange,
                        onRemove = onRemoveItem
                    )
                }
            }
        }
        
        Divider()
        
        // Totals Summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatCurrency(cartState.subtotal),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tax:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatCurrency(cartState.tax),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Grand Total:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatCurrency(cartState.grandTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onFinalizeOrder,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = cartState.items.isNotEmpty()
            ) {
                Icon(Icons.Default.CheckCircle, "Finalize")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalize Order", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = cartItem.menuItem.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatCurrency(cartItem.menuItem.price)} × ${cartItem.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tax: ${(cartItem.menuItem.taxRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onQuantityChange(cartItem.menuItem.itemId, cartItem.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(18.dp))
                }
                
                Text(
                    text = cartItem.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                IconButton(
                    onClick = { onQuantityChange(cartItem.menuItem.itemId, cartItem.quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(18.dp))
                }
                
                IconButton(
                    onClick = { onRemove(cartItem.menuItem.itemId) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, "Remove", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuPane(
    categories: List<String>,
    selectedCategory: String?,
    menuItems: List<MenuItemEntity>,
    onCategorySelected: (String?) -> Unit,
    onItemSelected: (MenuItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Divider()
        
        // Category Filter
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
        
        Divider()
        
        // Menu Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { item ->
                MenuItemCard(
                    item = item,
                    onAddClick = { onItemSelected(item) }
                )
            }
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
        onClick = if (item.isAvailable) onAddClick else { {} },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAvailable) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            item.itemNameMm?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Text(
                text = formatCurrency(item.price),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Tax: ${(item.taxRate * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            if (!item.isAvailable) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Out of Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalizeOrderDialog(
    cartState: CartState,
    tableName: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalize Order") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tableName?.let {
                    Text("Table: $it", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Items: ${cartState.items.size}", style = MaterialTheme.typography.bodyLarge)
                Text("Subtotal: ${formatCurrency(cartState.subtotal)}", style = MaterialTheme.typography.bodyLarge)
                Text("Tax: ${formatCurrency(cartState.tax)}", style = MaterialTheme.typography.bodyLarge)
                Divider()
                Text(
                    "Grand Total: ${formatCurrency(cartState.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
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

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "MM")).format(amount)
}
