package com.cosmicforge.pos.ui.waiter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.cosmicforge.pos.data.database.entities.TableEntity

/**
 * Floor map screen with real-time table status updates
 * Color-coded: Green=Free, Red=In Use, Orange=Dirty, Purple=Reserved
 */
@Composable
fun FloorMapScreen(
    onTableSelected: (TableEntity) -> Unit,
    viewModel: FloorMapViewModel = hiltViewModel()
) {
    val selectedFloor by viewModel.selectedFloor.collectAsState()
    val tables by viewModel.tablesOnFloor.collectAsState()
    val stats by viewModel.tableStats.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with stats
        FloorMapHeader(
            selectedFloor = selectedFloor,
            stats = stats,
            onFloorChange = { viewModel.selectFloor(it) }
        )
        
        Divider()
        
        // Table grid
        if (tables.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tables on this floor",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tables) { table ->
                    TableCard(
                        table = table,
                        onClick = {
                            if (table.status == TableEntity.STATUS_FREE) {
                                viewModel.openTable(table.tableId) {
                                    onTableSelected(table)
                                }
                            } else {
                                onTableSelected(table)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorMapHeader(
    selectedFloor: Int,
    stats: TableStats,
    onFloorChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Floor Map",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Floor selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..3).forEach { floor ->
                    FilterChip(
                        selected = selectedFloor == floor,
                        onClick = { onFloorChange(floor) },
                        label = { Text("Floor $floor") }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip("Free", stats.free, Color(0xFF4CAF50))
            StatChip("In Use", stats.inUse, Color(0xFFF44336))
            StatChip("Dirty", stats.dirty, Color(0xFFFF9800))
            StatChip("Reserved", stats.reserved, Color(0xFF9C27B0))
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TableCard(
    table: TableEntity,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = getTableColor(table.status),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tableColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getTableIcon(table.status),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "${table.capacity}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Text(
                text = table.tableName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = getStatusText(table.status),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun getTableColor(status: String): Color {
    return when (status) {
        TableEntity.STATUS_FREE -> Color(0xFF4CAF50) // Green
        TableEntity.STATUS_IN_USE -> Color(0xFFF44336) // Red
        TableEntity.STATUS_DIRTY -> Color(0xFFFF9800) // Orange
        TableEntity.STATUS_RESERVED -> Color(0xFF9C27B0) // Purple
        else -> Color.Gray
    }
}

@Composable
private fun getTableIcon(status: String) = when (status) {
    TableEntity.STATUS_FREE -> Icons.Default.CheckCircle
    TableEntity.STATUS_IN_USE -> Icons.Default.Restaurant
    TableEntity.STATUS_DIRTY -> Icons.Default.CleaningServices
    TableEntity.STATUS_RESERVED -> Icons.Default.BookmarkAdded
    else -> Icons.Default.TableRestaurant
}

private fun getStatusText(status: String): String {
    return when (status) {
        TableEntity.STATUS_FREE -> "Available"
        TableEntity.STATUS_IN_USE -> "Occupied"
        TableEntity.STATUS_DIRTY -> "Needs Cleaning"
        TableEntity.STATUS_RESERVED -> "Reserved"
        else -> "Unknown"
    }
}
