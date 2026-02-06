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

import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.rms.ui.viewmodels.DiagnosticsViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun DiagnosticsScreen(
    currentUser: UserEntity,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val metrics by viewModel.metrics.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.headlineMedium
            )
            
            IconButton(onClick = { viewModel.refreshMetrics() }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
        
        Text(
            text = "Auto-refreshes every 30 seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stone Vault Health
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stone Vault Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                MetricRow(
                    label = "Total Staff",
                    value = "${metrics.totalUsers}",
                    icon = Icons.Default.People
                )
                
                MetricRow(
                    label = "Active Staff",
                    value = "${metrics.activeUsers}",
                    icon = Icons.Default.Person
                )
                
                MetricRow(
                    label = "Total Orders",
                    value = "${metrics.totalOrders}",
                    icon = Icons.Default.Receipt
                )
                
                MetricRow(
                    label = "Pending Sync",
                    value = "${metrics.pendingSync}",
                    icon = Icons.Default.Sync,
                    valueColor = if (metrics.pendingSync > 0) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Subscription & Device Monitoring
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (metrics.isAtCapacity) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subscription Tier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = metrics.subscriptionTier.tierName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Device count with capacity warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tablet,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Devices:")
                    }
                    
                    Text(
                        text = "${metrics.activeDevices} / ${metrics.subscriptionTier.maxDevices}",
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            metrics.isAtCapacity -> MaterialTheme.colorScheme.error
                            metrics.activeDevices >= metrics.subscriptionTier.maxDevices - 2 -> Color(0xFFFF9800) // Orange warning
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Progress bar
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = metrics.activeDevices.toFloat() / metrics.subscriptionTier.maxDevices,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        metrics.isAtCapacity -> MaterialTheme.colorScheme.error
                        metrics.activeDevices >= metrics.subscriptionTier.maxDevices - 2 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                
                // Capacity warning
                if (metrics.isAtCapacity) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CAPACITY REACHED - Contact Architect for Gold Upgrade",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
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
