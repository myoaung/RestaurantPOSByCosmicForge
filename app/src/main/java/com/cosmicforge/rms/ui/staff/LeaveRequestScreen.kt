package com.cosmicforge.rms.ui.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cosmicforge.rms.data.database.entities.LeaveRequestEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.ui.viewmodels.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Leave Request Screen (Staff View)
 * v10.5: Leave Management
 * 
 * All roles can submit leave requests
 */
@Composable
fun LeaveRequestScreen(
    currentUser: UserEntity,
    viewModel: LeaveViewModel = hiltViewModel()
) {
    val myLeaves by viewModel.getMyLeaveRequests(currentUser.id).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Leave Requests") },
                actions = {
                    IconButton(onClick = { /* TODO: Leave balance */ }) {
                        Icon(Icons.Default.Info, "Leave Balance")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Request Leave")
            }
        }
    ) { padding ->
        if (myLeaves.isEmpty()) {
            EmptyLeaveState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myLeaves) { leaveRequest ->
                    LeaveRequestCard(leaveRequest = leaveRequest)
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddLeaveRequestDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { type, startDate, endDate, reason ->
                viewModel.submitLeaveRequest(
                    userId = currentUser.id,
                    leaveType = type,
                    startDate = startDate,
                    endDate = endDate,
                    reason = reason,
                    onSuccess = { showAddDialog = false },
                    onError = { /* TODO: Show error */ }
                )
            }
        )
    }
}

@Composable
fun LeaveRequestCard(leaveRequest: LeaveRequestEntity) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val startDateStr = sdf.format(Date(leaveRequest.startDate))
    val endDateStr = sdf.format(Date(leaveRequest.endDate))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (leaveRequest.status) {
                LeaveRequestEntity.STATUS_APPROVED -> MaterialTheme.colorScheme.primaryContainer
                LeaveRequestEntity.STATUS_REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = leaveRequest.leaveType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(status = leaveRequest.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$startDateStr - $endDateStr",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (leaveRequest.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${leaveRequest.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, icon) = when (status) {
        LeaveRequestEntity.STATUS_APPROVED -> Pair(Color(0xFF4CAF50), Icons.Default.CheckCircle)
        LeaveRequestEntity.STATUS_REJECTED -> Pair(MaterialTheme.colorScheme.error, Icons.Default.Cancel)
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Pending)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AddLeaveRequestDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Long, Long, String) -> Unit
) {
    var leaveType by remember { mutableStateOf(LeaveRequestEntity.TYPE_ANNUAL) }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val leaveTypes = listOf(
        LeaveRequestEntity.TYPE_ANNUAL,
        LeaveRequestEntity.TYPE_SICK,
        LeaveRequestEntity.TYPE_EMERGENCY
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Leave") },
        text = {
            Column {
                // Leave Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = leaveType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Leave Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        leaveTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    leaveType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // TODO: Date pickers for startDate and endDate
                Text(
                    text = "Date selection: Use current date for now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(leaveType, startDate, endDate, reason)
            }) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyLeaveState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BeachAccess,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Leave Requests",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap + to request leave",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
