@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmicforge.pos.ui.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for manual parcel fee override
 */
@Composable
fun ParcelFeeOverrideDialog(
    currentFee: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var customFee by remember { mutableStateOf(currentFee.toString()) }
    var useDefault by remember { mutableStateOf(currentFee == 1000.0) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adjust Parcel Fee",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Default: 1,000 MMK",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useDefault,
                        onCheckedChange = {
                            useDefault = it
                            if (it) {
                                customFee = "1000"
                            }
                        }
                    )
                    Text("Use default fee")
                }
                
                OutlinedTextField(
                    value = customFee,
                    onValueChange = {
                        customFee = it
                        useDefault = false
                    },
                    label = { Text("Custom Fee") },
                    suffix = { Text("MMK") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !useDefault
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ This adjustment will be logged in the audit trail",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val fee = if (useDefault) {
                                null // Use default
                            } else {
                                customFee.toDoubleOrNull()
                            }
                            onConfirm(fee)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Parcel fee display with edit button
 */
@Composable
fun ParcelFeeRow(
    parcelFee: Double,
    isCustom: Boolean,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Parcel Fee:", style = MaterialTheme.typography.bodyLarge)
                if (isCustom) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text("Custom", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (isCustom) {
                Text(
                    "(Manual override - will be audited)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${parcelFee.toInt()} MMK",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCustom)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.primary
            )
            
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edit fee")
            }
        }
    }
}
