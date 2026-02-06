package com.cosmicforge.rms.ui.admin

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
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.ui.viewmodels.StaffViewModel
import com.cosmicforge.rms.utils.StaffUtils

/**
 * Staff Management Screen
 * v9.7: Staff Management Foundation
 * 
 * RBAC: Manager/Owner only
 */
@Composable
fun StaffManagementScreen(
    currentUser: UserEntity,
    viewModel: StaffViewModel = hiltViewModel()
) {
    // RBAC Gate: Only Manager (2) and Owner (1) can access
    if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
        AccessDeniedScreen()
        return
    }
    
    val staffList by viewModel.getAllStaff(currentUser).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Management") },
                navigationIcon = {
                    Icon(Icons.Default.People, contentDescription = null)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Staff")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(staffList) { staff ->
                StaffCard(
                    staff = staff,
                    currentUser = currentUser,
                    viewModel = viewModel
                )
            }
        }
    }
    
    if (showAddDialog) {
        AddStaffDialog(
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun StaffCard(
    staff: UserEntity,
    currentUser: UserEntity,
    viewModel: StaffViewModel
) {
    val tenure = StaffUtils.calculateTenure(staff.joinDate)
    val tenureText = StaffUtils.formatTenure(tenure)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = staff.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = staff.getRoleName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // NRC (Owner/Manager only)
                    if (!staff.nrcNumber.isNullOrBlank()) {
                        Text(
                            text = "NRC: ${staff.nrcNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "Tenure: $tenureText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons (Owner only)
                if (currentUser.roleLevel == UserEntity.ROLE_OWNER) {
                    IconButton(onClick = { /* TODO: Deactivate */ }) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = "Deactivate",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddStaffDialog(
    currentUser: UserEntity,
    viewModel: StaffViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var nrc by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserEntity.ROLE_WAITER) }
    var showError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Staff") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN (4 digits)") },
               modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., 1234") }
                )
                
                OutlinedTextField(
                    value = nrc,
                    onValueChange = { nrc = it },
                    label = { Text("Myanmar NRC (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., 12/TaKaNa(N)123456") }
                )
                
                // Role dropdown
                Text("Role", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleChip("Manager", UserEntity.ROLE_MANAGER, selectedRole) { selectedRole = it }
                    RoleChip("Waiter", UserEntity.ROLE_WAITER, selectedRole) { selectedRole = it }
                    RoleChip("Chief", UserEntity.ROLE_CHIEF, selectedRole) { selectedRole = it }
                }
                
                // Error message
                if (showError != null) {
                    Text(
                        text = showError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                  isLoading = true
                    viewModel.createStaff(
                        userName = name,
                        pin = pin,
                        roleLevel = selectedRole,
                        nrcNumber = nrc.ifBlank { null },
                        currentUser = currentUser,
                        onSuccess = {
                            isLoading = false
                            onDismiss()
                        },
                        onError = { error ->
                            isLoading = false
                            showError = error
                        }
                    )
                },
                enabled = !isLoading && name.isNotBlank() && pin.length == 4
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Add Staff")
                }
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
fun RoleChip(
    label: String,
    role: Int,
    selectedRole: Int,
    onSelect: (Int) -> Unit
) {
    FilterChip(
        selected = selectedRole == role,
        onClick = { onSelect(role) },
        label = { Text(label) }
    )
}

@Composable
fun AccessDeniedScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Access Denied",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Only Manager and Owner can access Staff Management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
