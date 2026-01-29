@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmicforge.pos.ui.settings

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
import com.cosmicforge.pos.data.database.entities.SMSTemplateEntity
import com.cosmicforge.pos.data.database.entities.UserEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * SMS Template Editor Screen
 * RBAC: Owner (Level 1) or Manager (Level 2) only
 */
@Composable
fun SMSTemplateEditorScreen(
    currentUser: UserEntity,
    onNavigateBack: () -> Unit,
    viewModel: SMSTemplateViewModel = hiltViewModel()
) {
    // RBAC Check - Only Owner or Manager
    if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
        AccessDeniedScreen(onNavigateBack)
        return
    }
    
    val templates by viewModel.templates.collectAsState()
    var selectedTemplateType by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    
                    Column {
                        Text(
                            text = "SMS Template Editor",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize Myanmar Unicode messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Owner/Manager Only")
                }
            }
        }
        
        // Template list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                TemplateCard(
                    template = template,
                    displayName = viewModel.getTemplateDisplayName(template.templateType),
                    onEdit = {
                        selectedTemplateType = template.templateType
                        showEditor = true
                    }
                )
            }
        }
    }
    
    // Template editor dialog
    if (showEditor && selectedTemplateType != null) {
        TemplateEditorDialog(
            templateType = selectedTemplateType!!,
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
private fun TemplateCard(
    template: SMSTemplateEntity,
    displayName: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    template.updatedBy?.let { updatedBy ->
                        Text(
                            text = "Last edited by: $updatedBy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
            
            Divider()
            
            // Preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = template.templateText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemplateEditorDialog(
    templateType: String,
    currentUser: UserEntity,
    viewModel: SMSTemplateViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(templateType) {
        viewModel.selectTemplate(templateType)
    }
    
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val availableVars = viewModel.getAvailableVariables(templateType)
    
    var editedText by remember { mutableStateOf(selectedTemplate?.templateText ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let {
            editedText = it.templateText
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getTemplateDisplayName(templateType),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Divider()
                
                // Available variables
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                "Info",
                                tint = Color(0xFFF57C00)
                            )
                            Text(
                                "Available Variables",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        availableVars.forEach { variable ->
                            Text(
                                text = "• $variable",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }
                }
                
                // Text editor
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("Message Template (Myanmar Unicode)") },
                    placeholder = { Text("Enter your custom message...") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    minLines = 10
                )
                
                // Error message
                errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetToDefault(templateType)
                            editedText = SMSTemplateEntity.getDefaultTemplates()
                                .find { it.templateType == templateType }
                                ?.templateText ?: ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, "Reset")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Default")
                    }
                    
                    Button(
                        onClick = {
                            viewModel.updateTemplate(
                                templateType = templateType,
                                newText = editedText,
                                updatedBy = currentUser.userName,
                                onSuccess = {
                                    showSuccessSnackbar = true
                                    onDismiss()
                                },
                                onError = { error ->
                                    errorMessage = error
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, "Save")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessDeniedScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                "Access Denied",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "Access Denied",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Only Owner or Manager can edit SMS templates",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Go Back")
            }
        }
    }
}
