package com.cosmicforge.rms.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cosmicforge.rms.data.database.entities.UserEntity

/**
 * Architect Dashboard with NavigationRail
 * v10.3: Professional Navigation System
 * 
 * RBAC: Architect/Owner/Manager
 */
@Composable
fun ArchitectDashboardScreen(
    currentUser: UserEntity
) {
    var selectedTab by remember { mutableStateOf(ArchitectTab.STAFF) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail
        NavigationRail(
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            ArchitectTab.values().forEach { tab ->
                NavigationRailItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            content Description = tab.label
                        )
                    },
                    label = { Text(tab.label) }
                )
            }
        }
        
        // Content Area
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                ArchitectTab.STAFF -> StaffManagementScreen(currentUser = currentUser)
                ArchitectTab.REWARDS -> RewardHistoryScreen(currentUser = currentUser)
                ArchitectTab.DIAGNOSTICS -> DiagnosticsScreen(currentUser = currentUser)
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(currentUser: UserEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "System Diagnostics",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stone Vault Health",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Database Status:")
                    Text(
                        text = "✅ Healthy",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Devices:")
                    Text(
                        text = "1 / 8",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subscription Tier:")
                    Text(
                        text = "Silver (8 tablets)",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Advanced diagnostics coming in v10.7",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Navigation tabs for Architect Dashboard
 */
enum class ArchitectTab(
    val label: String,
    val icon: ImageVector
) {
    STAFF("Staff", Icons.Default.People),
    REWARDS("Rewards", Icons.Default.EmojiEvents),
    DIAGNOSTICS("Diagnostics", Icons.Default.HealthAndSafety)
}
