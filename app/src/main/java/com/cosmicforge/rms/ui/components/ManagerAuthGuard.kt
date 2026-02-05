package com.cosmicforge.rms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Manager Authentication Guard
 * 
 * Security Gate Requirement: "Access to DeadLetterVault must remain restricted 
 * until the ManagerAuth event is captured"
 * 
 * Implements 6-digit PIN authentication for manager-level features
 */
@Composable
fun ManagerAuthGuard(
    pinVerificationUtil: com.cosmicforge.rms.core.security.PinVerificationUtil,
    onAuthenticated: (com.cosmicforge.rms.data.database.entities.UserEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    
    // Coroutine scope for async PIN verification
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.width(320.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lock icon
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Manager Access",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                // Title
                Text(
                    text = "Manager Authentication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Enter 6-digit Manager PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // PIN input field
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("Manager PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // PIN dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (index < pin.length) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            isVerifying = true
                            error = null
                            
                            // SECURITY GATE: Verify PIN against UserEntity (Manager/Owner role)
                            scope.launch {
                                try {
                                    val authenticatedUser = pinVerificationUtil.verifyManagerPin(pin)
                                    
                                    if (authenticatedUser != null) {
                                        // PIN valid and user has manager access
                                        android.util.Log.d("ManagerAuth", "✅ Manager authenticated: ${authenticatedUser.userName}")
                                        onAuthenticated(authenticatedUser)
                                    } else {
                                        // Invalid PIN or insufficient privileges
                                        error = "Invalid PIN or insufficient privileges"
                                        isVerifying = false
                                        android.util.Log.w("ManagerAuth", "❌ Authentication failed")
                                    }
                                } catch (e: Exception) {
                                    error = "Authentication error: ${e.message}"
                                    isVerifying = false
                                    android.util.Log.e("ManagerAuth", "Error during authentication", e)
                                }
                            }
                        },
                        enabled = pin.length == 6 && !isVerifying,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Unlock")
                        }
                    }
                }
                
                // Security notice
                Text(
                    text = "⚠️ Manager access only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Wrapper composable that guards content behind manager authentication
 * 
 * Usage: Inject PinVerificationUtil via Hilt in your screen composable
 */
@Composable
fun ManagerProtectedContent(
    pinVerificationUtil: com.cosmicforge.rms.core.security.PinVerificationUtil,
    content: @Composable (com.cosmicforge.rms.data.database.entities.UserEntity) -> Unit
) {
    var authenticatedUser by remember { mutableStateOf<com.cosmicforge.rms.data.database.entities.UserEntity?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    
    if (authenticatedUser != null) {
        content(authenticatedUser!!)
    } else {
        // Show locked screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "Manager Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "This feature requires manager authentication",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(onClick = { showAuthDialog = true }) {
                    Text("Authenticate")
                }
            }
        }
        
        if (showAuthDialog) {
            ManagerAuthGuard(
                pinVerificationUtil = pinVerificationUtil,
                onAuthenticated = { user ->
                    authenticatedUser = user
                    showAuthDialog = false
                },
                onDismiss = { showAuthDialog = false }
            )
        }
    }
}
