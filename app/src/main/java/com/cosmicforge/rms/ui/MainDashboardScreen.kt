package com.cosmicforge.rms.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.rms.BuildConfig
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.ui.kds.KDSScreen
import com.cosmicforge.rms.ui.owner.OwnerDashboardScreen
import com.cosmicforge.rms.ui.waiter.FloorMapScreen
import com.cosmicforge.rms.ui.waiter.OrderEntryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    currentUser: UserEntity,
    onLogout: () -> Unit,
    viewModel: MainDashboardViewModel = hiltViewModel()
) {
    var selectedRoute by remember { mutableStateOf(NavigationRoute.FLOOR_MAP) }
    var selectedTableId by remember { mutableStateOf<String?>(null) }
    var selectedTableName by remember { mutableStateOf<String?>(null) }
    var isRailExpanded by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Expandable Navigation Rail
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isRailExpanded) 200.dp else 80.dp),
            header = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hamburger toggle button
                    IconButton(onClick = { isRailExpanded = !isRailExpanded }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Menu",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Profile Header with Dropdown
                    Box {
                        Column(
                            modifier = Modifier
                                .clickable { showProfileMenu = !showProfileMenu }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Cosmic Forge RMS",
                                modifier = Modifier.size(if (isRailExpanded) 40.dp else 32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            if (isRailExpanded) {
                                Text(
                                    text = currentUser.userName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getRoleLabel(currentUser.roleLevel),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        // Profile Dropdown Menu
                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Settings") },
                                onClick = {
                                    showProfileMenu = false
                                    selectedRoute = NavigationRoute.ADMIN
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            
                            Divider()
                            
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showProfileMenu = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                                }
                            )
                            
                            Divider()
                            
                            // Version display
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Version: ${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { },
                                enabled = false
                            )
                        }
                    }
                }
            }
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Get visible routes based on user role
            val visibleRoutes = getVisibleRoutes(currentUser.roleLevel)
            
            // OPERATIONS SECTION (Top of sidebar)
            val operationsRoutes = visibleRoutes.filter { 
                it == NavigationRoute.FLOOR_MAP || it == NavigationRoute.ORDERS || it == NavigationRoute.KDS 
            }
            
            operationsRoutes.forEach { route ->
                NavigationRailItem(
                    icon = { Icon(route.icon, contentDescription = route.label) },
                    label = if (isRailExpanded) {
                        { Text(route.label) }
                    } else null,
                    selected = selectedRoute == route,
                    onClick = { 
                        selectedRoute = route 
                        if (route != NavigationRoute.ORDERS) {
                            selectedTableId = null
                            selectedTableName = null
                        }
                    },
                    alwaysShowLabel = isRailExpanded
                )
            }
            
            // Spacer pushes Settings section to bottom
            Spacer(modifier = Modifier.weight(1f))
            
            // Visual separator
            if (visibleRoutes.contains(NavigationRoute.ADMIN)) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
            
            // SETTINGS SECTION (Bottom of sidebar)
            val settingsRoutes = visibleRoutes.filter { it == NavigationRoute.ADMIN }
            
            settingsRoutes.forEach { route ->
                NavigationRailItem(
                    icon = { Icon(route.icon, contentDescription = route.label) },
                    label = if (isRailExpanded) {
                        { Text(route.label) }
                    } else null,
                    selected = selectedRoute == route,
                    onClick = { selectedRoute = route },
                    alwaysShowLabel = isRailExpanded
                )
            }
        }
        
        // Main content area
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedRoute) {
                NavigationRoute.FLOOR_MAP -> FloorMapScreen(
                    onTableSelected = { table ->
                        selectedTableId = table.tableId
                        selectedTableName = table.tableName
                        selectedRoute = NavigationRoute.ORDERS
                    }
                )
                NavigationRoute.ORDERS -> OrderEntryScreen(
                    currentUser = currentUser,
                    tableId = selectedTableId,
                    tableName = selectedTableName,
                    onBack = {
                        selectedTableId = null
                        selectedTableName = null
                        selectedRoute = NavigationRoute.FLOOR_MAP
                    }
                )
                NavigationRoute.KDS -> KDSScreen(currentUser = currentUser)
                NavigationRoute.ADMIN -> OwnerDashboardScreen()
            }
        }
    }
}

/**
 * Get role label for display
 */
private fun getRoleLabel(roleLevel: Int): String {
    return when (roleLevel) {
        UserEntity.ROLE_OWNER -> "Owner"
        UserEntity.ROLE_MANAGER -> "Manager"
        UserEntity.ROLE_WAITER -> "Waiter"
        UserEntity.ROLE_CHIEF -> "Chief"
        else -> "User"
    }
}

/**
 * Get visible navigation routes based on user role
 */
private fun getVisibleRoutes(roleLevel: Int): List<NavigationRoute> {
    return when (roleLevel) {
        UserEntity.ROLE_OWNER -> listOf(
            NavigationRoute.FLOOR_MAP,
            NavigationRoute.ORDERS,
            NavigationRoute.KDS,
            NavigationRoute.ADMIN
        )
        UserEntity.ROLE_MANAGER -> listOf(
            NavigationRoute.FLOOR_MAP,
            NavigationRoute.ORDERS,
            NavigationRoute.KDS,
            NavigationRoute.ADMIN
        )
        UserEntity.ROLE_WAITER -> listOf(
            NavigationRoute.FLOOR_MAP,
            NavigationRoute.ORDERS
        )
        UserEntity.ROLE_CHIEF -> listOf(
            NavigationRoute.KDS
        )
        else -> emptyList()
    }
}

/**
 * Navigation routes for the dashboard
 */
enum class NavigationRoute(
    val label: String,
    val icon: ImageVector
) {
    FLOOR_MAP("Floor Map", Icons.Default.GridView),
    ORDERS("Orders", Icons.Default.Receipt),
    KDS("Kitchen", Icons.Default.Kitchen),
    ADMIN("Admin", Icons.Default.Settings)
}
