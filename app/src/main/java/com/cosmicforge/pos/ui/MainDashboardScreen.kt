package com.cosmicforge.pos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.pos.data.database.entities.UserEntity
import com.cosmicforge.pos.ui.kds.KDSScreen
import com.cosmicforge.pos.ui.owner.OwnerDashboardScreen
import com.cosmicforge.pos.ui.waiter.FloorMapScreen
import com.cosmicforge.pos.ui.waiter.OrderEntryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    currentUser: UserEntity,
    onLogout: () -> Unit,
    viewModel: MainDashboardViewModel = hiltViewModel()
) {
    var selectedRoute by remember { mutableStateOf(NavigationRoute.FLOOR_MAP) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail (Sidebar)
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            header = {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Cosmic Forge POS",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = currentUser.userName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Get visible routes based on user role
            val visibleRoutes = getVisibleRoutes(currentUser.roleLevel)
            
            visibleRoutes.forEach { route ->
                NavigationRailItem(
                    icon = { Icon(route.icon, contentDescription = route.label) },
                    label = { Text(route.label) },
                    selected = selectedRoute == route,
                    onClick = { selectedRoute = route }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout button at bottom
            NavigationRailItem(
                icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout") },
                label = { Text("Logout") },
                selected = false,
                onClick = onLogout
            )
        }
        
        // Main content area
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedRoute) {
                NavigationRoute.FLOOR_MAP -> FloorMapScreen()
                NavigationRoute.ORDERS -> OrderEntryScreen()
                NavigationRoute.KDS -> KDSScreen()
                NavigationRoute.ADMIN -> OwnerDashboardScreen()
            }
        }
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
    FLOOR_MAP("Floor Map", Icons.Default.TableRestaurant),
    ORDERS("Orders", Icons.Default.Receipt),
    KDS("Kitchen", Icons.Default.Kitchen),
    ADMIN("Admin", Icons.Default.Settings)
}

// Placeholder icon for TableRestaurant (using GridView as substitute)
private val Icons.Default.TableRestaurant: ImageVector
    get() = Icons.Default.GridView
