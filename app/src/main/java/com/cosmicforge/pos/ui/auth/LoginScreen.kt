package com.cosmicforge.pos.ui.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmicforge.pos.data.database.entities.UserEntity

/**
 * Login screen with user selection and PIN entry
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserEntity) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is LoginUiState.SelectUser -> {
                UserSelectionScreen(
                    viewModel = viewModel,
                    onUserSelected = { viewModel.selectUser(it) }
                )
            }
            is LoginUiState.EnterPin -> {
                PinEntryScreen(
                    user = state.user,
                    onPinEntered = { viewModel.login(it) },
                    onBack = { viewModel.backToUserSelection() }
                )
            }
            is LoginUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoginUiState.Success -> {
                LaunchedEffect(Unit) {
                    onLoginSuccess(state.user)
                }
            }
            is LoginUiState.Error -> {
                // Error will auto-clear to PIN entry
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSelectionScreen(
    viewModel: LoginViewModel,
    onUserSelected: (UserEntity) -> Unit
) {
    val users by viewModel.activeUsers.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "🚀 Cosmic Forge POS",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Select Your Profile",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onClick = { onUserSelected(user) }
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getRoleIcon(user.roleLevel),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = getRoleColor(user.roleLevel)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = user.userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = user.getRoleName(),
                style = MaterialTheme.typography.bodyMedium,
                color = getRoleColor(user.roleLevel)
            )
        }
    }
}

@Composable
private fun PinEntryScreen(
    user: UserEntity,
    onPinEntered: (String) -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            imageVector = getRoleIcon(user.roleLevel),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = getRoleColor(user.roleLevel)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = user.userName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = user.getRoleName(),
            style = MaterialTheme.typography.titleMedium,
            color = getRoleColor(user.roleLevel)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Enter PIN",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // PIN display
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = if (index < pin.length)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < pin.length) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Number pad
        PinPad(
            onNumberClick = { number ->
                if (pin.length < 4) {
                    pin += number
                    if (pin.length == 4) {
                        onPinEntered(pin)
                        pin = "" // Reset for next attempt
                    }
                }
            },
            onClearClick = { pin = "" }
        )
    }
}

@Composable
private fun PinPad(
    onNumberClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("Clear", "0", "⌫")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    PinButton(
                        text = key,
                        onClick = {
                            when (key) {
                                "Clear" -> onClearClick()
                                "⌫" -> { /* Backspace */ }
                                else -> onNumberClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(96.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun getRoleIcon(roleLevel: Int) = when (roleLevel) {
    UserEntity.ROLE_OWNER -> Icons.Default.SupervisorAccount
    UserEntity.ROLE_MANAGER -> Icons.Default.ManageAccounts
    UserEntity.ROLE_WAITER -> Icons.Default.Restaurant
    UserEntity.ROLE_CHIEF -> Icons.Default.ChefHat
    else -> Icons.Default.Person
}

@Composable
private fun getRoleColor(roleLevel: Int) = when (roleLevel) {
    UserEntity.ROLE_OWNER -> MaterialTheme.colorScheme.primary
    UserEntity.ROLE_MANAGER -> MaterialTheme.colorScheme.secondary
    UserEntity.ROLE_WAITER -> MaterialTheme.colorScheme.tertiary
    UserEntity.ROLE_CHIEF -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

// Chef hat icon placeholder (Material Icons doesn't have this, using Kitchen instead)
private val Icons.Default.ChefHat
    get() = Icons.Default.Kitchen
