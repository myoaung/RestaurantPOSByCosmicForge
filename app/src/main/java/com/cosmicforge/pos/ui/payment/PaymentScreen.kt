package com.cosmicforge.pos.ui.payment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter

/**
 * Payment screen with proof capture for digital payments
 */
@Composable
fun PaymentScreen(
    orderId: Long,
    totalAmount: Double,
    onPaymentCompleted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val selectedMethod by viewModel.selectedPaymentMethod.collectAsState()
    val paymentProofUri by viewModel.paymentProofUri.collectAsState()
    val amountReceived by viewModel.amountReceived.collectAsState()
    
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Camera launcher for payment proof
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            viewModel.setPaymentProof(null)
        }
    }
    
    // Gallery launcher for payment proof
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setPaymentProof(uri)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        PaymentHeader(
            totalAmount = totalAmount,
            onBack = onCancel
        )
        
        Divider()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Payment method selector
            PaymentMethodSelector(
                selectedMethod = selectedMethod,
                onMethodSelected = { viewModel.selectPaymentMethod(it) }
            )
            
            when (selectedMethod) {
                PaymentMethod.CASH -> {
                    CashPaymentSection(
                        totalAmount = totalAmount,
                        amountReceived = amountReceived,
                        onAmountChange = { viewModel.setAmountReceived(it) },
                        change = viewModel.calculateChange(totalAmount)
                    )
                }
                PaymentMethod.KPAY, PaymentMethod.CBPAY -> {
                    DigitalPaymentSection(
                        paymentMethod = selectedMethod,
                        totalAmount = totalAmount,
                        paymentProofUri = paymentProofUri,
                        onCameraClick = {
                            // TODO: Create temp file for camera
                            // cameraLauncher.launch(tempUri)
                        },
                        onGalleryClick = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Error message
            showError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Complete payment button
            Button(
                onClick = {
                    viewModel.processPayment(
                        orderId = orderId,
                        totalAmount = totalAmount,
                        onSuccess = onPaymentCompleted,
                        onError = { showError = it }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.CheckCircle, "Complete")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Payment")
            }
        }
    }
}

@Composable
private fun PaymentHeader(
    totalAmount: Double,
    onBack: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text(
                    text = "Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${totalAmount.toInt()} MMK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Payment Method",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentMethod.values().forEach { method ->
                PaymentMethodCard(
                    method = method,
                    isSelected = selectedMethod == method,
                    onClick = { onMethodSelected(method) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getPaymentIcon(method),
                contentDescription = method.displayName,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = method.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CashPaymentSection(
    totalAmount: Double,
    amountReceived: String,
    onAmountChange: (String) -> Unit,
    change: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = amountReceived,
            onValueChange = onAmountChange,
            label = { Text("Amount Received") },
            placeholder = { Text("Enter amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("MMK") },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (change > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Change:",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${change.toInt()} MMK",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalPaymentSection(
    paymentMethod: PaymentMethod,
    totalAmount: Double,
    paymentProofUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Payment Proof Required",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Capture screenshot of ${paymentMethod.displayName} transaction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (paymentProofUri != null) {
            // Show captured proof
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(paymentProofUri),
                    contentDescription = "Payment Proof",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            TextButton(onClick = { onGalleryClick() }) {
                Icon(Icons.Default.Refresh, "Change")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change Proof")
            }
        } else {
            // Capture buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraClick,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Camera")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }
                
                OutlinedButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        }
    }
}

@Composable
private fun getPaymentIcon(method: PaymentMethod) = when (method) {
    PaymentMethod.CASH -> Icons.Default.Money
    PaymentMethod.KPAY -> Icons.Default.Payment
    PaymentMethod.CBPAY -> Icons.Default.CreditCard
}
