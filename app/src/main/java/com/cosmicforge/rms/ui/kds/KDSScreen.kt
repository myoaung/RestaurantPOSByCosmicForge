@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmicforge.rms.ui.kds

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.cosmicforge.rms.data.database.entities.OrderDetailEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Kitchen Display System with 8-chief claim accountability
 */
@Composable
fun KDSScreen(
    currentUser: UserEntity,
    viewModel: KDSViewModel = hiltViewModel()
) {
    LaunchedEffect(currentUser) {
        viewModel.setCurrentUser(currentUser)
    }
    
    val selectedStation by viewModel.selectedStation.collectAsState()
    val filteredOrders by viewModel.filteredOrders.collectAsState()
    
    val stations = listOf("KITCHEN", "GRILL", "FRYER", "SALAD", "DESSERT")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with station filter
        KDSHeader(
            chiefName = currentUser.userName,
            selectedStation = selectedStation,
            stations = stations,
            onStationSelected = { viewModel.selectStation(it) }
        )
        
        Divider()
        
        // Ticket grid
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No pending orders",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredOrders) { ticket ->
                    OrderTicketCard(
                        ticket = ticket,
                        chiefId = currentUser.userId,
                        chiefName = currentUser.userName,
                        onClaimDetail = { detailId ->
                            viewModel.claimOrderDetail(
                                orderId = ticket.order.orderId,
                                detailId = detailId,
                                chiefId = currentUser.userId,
                                chiefName = currentUser.userName
                            )
                        },
                        onMarkReady = { detailId ->
                            viewModel.markDetailReady(
                                detailId = detailId,
                                chiefId = currentUser.userId,
                                chiefName = currentUser.userName
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KDSHeader(
    chiefName: String,
    selectedStation: String?,
    stations: List<String>,
    onStationSelected: (String?) -> Unit
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
            Column {
                Text(
                    text = "Kitchen Display",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chief: $chiefName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Station filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedStation == null,
                    onClick = { onStationSelected(null) },
                    label = { Text("All Stations") }
                )
            }
            
            items(stations) { station ->
                FilterChip(
                    selected = selectedStation == station,
                    onClick = { onStationSelected(station) },
                    label = { Text(station) }
                )
            }
        }
    }
}

@Composable
private fun OrderTicketCard(
    ticket: KDSTicket,
    chiefId: Long,
    chiefName: String,
    onClaimDetail: (Long) -> Unit,
    onMarkReady: (Long) -> Unit
) {
    val elapsedMinutes = ticket.elapsedTime / 60000
    val isUrgent = elapsedMinutes > 15
    
    val cardColor by animateColorAsState(
        targetValue = when {
            isUrgent -> Color(0xFFFFEBEE) // Light red
            ticket.pendingCount == 0 -> Color(0xFFE8F5E9) // Light green
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "cardColor"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Order header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(
                        containerColor = if (isUrgent) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            ticket.order.orderNumber,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = if (ticket.order.tableId != null) 
                            "Table: ${ticket.order.tableId}" 
                        else 
                            "PARCEL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$elapsedMinutes min",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${ticket.readyCount}/${ticket.totalItems} ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider()
            
            // Order details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ticket.details.forEach { detail ->
                    OrderDetailRow(
                        detail = detail,
                        isMyDetail = detail.claimedById == chiefId,
                        onClaim = { onClaimDetail(detail.detailId) },
                        onMarkReady = { onMarkReady(detail.detailId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailRow(
    detail: OrderDetailEntity,
    isMyDetail: Boolean,
    onClaim: () -> Unit,
    onMarkReady: () -> Unit
) {
    val backgroundColor = when (detail.status) {
        "READY" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        "COOKING" -> if (isMyDetail) 
            Color(0xFFFF9800).copy(alpha = 0.2f) 
        else 
            Color(0xFF9E9E9E).copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = if (isMyDetail && detail.status == "COOKING") 
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF9800))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge {
                        Text("  ${detail.quantity}x", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = detail.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                detail.claimedBy?.let { chiefName ->
                    Text(
                        text = "👨‍🍳 $chiefName",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMyDetail) 
                            Color(0xFFFF9800) 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (detail.status == "COOKING" && detail.claimedAtTimestamp != null) {
                    val cookTime = (System.currentTimeMillis() - detail.claimedAtTimestamp) / 60000
                    Text(
                        text = "⏱️ ${cookTime}m cooking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action button
            when (detail.status) {
                "PENDING" -> {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.TouchApp, "Claim")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Claim")
                    }
                }
                "COOKING" -> {
                    if (isMyDetail) {
                        Button(
                            onClick = onMarkReady,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, "Ready")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ready")
                        }
                    } else {
                        Text(
                            text = "IN PROGRESS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                "READY" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            "Ready",
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            "READY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}
