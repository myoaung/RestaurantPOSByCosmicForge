package com.cosmicforge.pos.ui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.pos.data.repository.ChiefPerformance
import com.cosmicforge.pos.data.repository.ParcelAdjustment
import com.cosmicforge.pos.data.repository.SalesSummary
import java.text.SimpleDateFormat
import java.util.*

/**
 * Owner dashboard with sales analytics and audit features
 */
@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerDashboardViewModel = hiltViewModel()
) {
    val salesSummary by viewModel.salesSummary.collectAsState()
    val parcelAdjustments by viewModel.parcelAdjustments.collectAsState()
    val chiefPerformance by viewModel.chiefPerformance.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showEndOfDayDialog by remember { mutableStateOf(false) }
    var endOfDayReport by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        DashboardHeader(
            onRefresh = { viewModel.loadDashboardData() },
            onGenerateReport = {
                viewModel.generateEndOfDayReport { report ->
                    endOfDayReport = report
                    showEndOfDayDialog = true
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sales summary cards
                item {
                    salesSummary?.let { summary ->
                        SalesSummarySection(summary)
                    }
                }
                
                // Parcel adjustments audit
                item {
                    ParcelAdjustmentsSection(parcelAdjustments)
                }
                
                // Chief performance
                item {
                    ChiefPerformanceSection(chiefPerformance)
                }
            }
        }
    }
    
    // End-of-day report dialog
    if (showEndOfDayDialog && endOfDayReport != null) {
        EndOfDayReportDialog(
            report = endOfDayReport!!,
            onDismiss = { showEndOfDayDialog = false }
        )
    }
}

@Composable
private fun DashboardHeader(
    onRefresh: () -> Unit,
    onGenerateReport: () -> Unit
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
            Column {
                Text(
                    text = "Owner Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today's Performance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
                
                Button(onClick = onGenerateReport) {
                    Icon(Icons.Default.Description, "Report")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End of Day")
                }
            }
        }
    }
}

@Composable
private fun SalesSummarySection(summary: SalesSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Sales Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Total sales card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Sales",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${summary.totalSales.toInt()} MMK",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${summary.totalOrders} orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Payment method breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentMethodCard(
                label = "Cash",
                amount = summary.cashSales,
                icon = Icons.Default.Money,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            
            PaymentMethodCard(
                label = "KPay",
                amount = summary.kpaySales,
                icon = Icons.Default.Payment,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            
            PaymentMethodCard(
                label = "CB Pay",
                amount = summary.cbpaySales,
                icon = Icons.Default.CreditCard,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = "${amount.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ParcelAdjustmentsSection(adjustments: List<ParcelAdjustment>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Parcel Fee Adjustments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (adjustments.isNotEmpty()) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text("${adjustments.size} manual")
                }
            }
        }
        
        if (adjustments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "No adjustments",
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        "✓ No manual parcel fee adjustments today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    adjustments.forEach { adjustment ->
                        ParcelAdjustmentRow(adjustment)
                        if (adjustment != adjustments.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelAdjustmentRow(adjustment: ParcelAdjustment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = adjustment.orderNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "By: ${adjustment.adjustedBy}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            adjustment.customerName?.let {
                Text(
                    text = "Customer: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "1,000 → ${adjustment.customFee.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val sign = if (adjustment.difference >= 0) "+" else ""
            Text(
                text = "$sign${adjustment.difference.toInt()} MMK",
                style = MaterialTheme.typography.bodyMedium,
                color = if (adjustment.difference >= 0) 
                    Color(0xFF4CAF50) 
                else 
                    Color(0xFFF44336),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChiefPerformanceSection(performance: List<ChiefPerformance>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Chief Performance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        if (performance.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    performance.forEach { chief ->
                        ChiefPerformanceRow(chief)
                        if (chief != performance.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChiefPerformanceRow(chief: ChiefPerformance) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                "Chief",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = chief.chiefName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${chief.dishesCompleted} dishes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Avg: ${String.format("%.1f", chief.averageCookTimeMinutes)}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EndOfDayReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "End of Day Report",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Divider()
                
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF263238)
                    )
                ) {
                    SelectionContainer {
                        Text(
                            text = report,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Export report */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, "Export")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
