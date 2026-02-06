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
import com.cosmicforge.rms.data.database.entities.RewardEntity
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.ui.viewmodels.RewardViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reward History Screen
 * v10.0: Gamification Engine
 * 
 * RBAC: Manager/Owner only
 */
@Composable
fun RewardHistoryScreen(
    currentUser: UserEntity,
    viewModel: RewardViewModel = hiltViewModel()
) {
    // RBAC Gate: Only Manager and Owner
    if (currentUser.roleLevel > UserEntity.ROLE_MANAGER) {
        AccessDeniedScreen()
        return
    }
    
    val rewards by viewModel.getAllRewards(currentUser).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reward History") },
                navigationIcon = {
                    Icon(Icons.Default.Star, contentDescription = null)
                }
            )
        }
    ) { padding ->
        if (rewards.isEmpty()) {
            EmptyRewardsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rewards) { reward ->
                    RewardCard(
                        reward = reward,
                        currentUser = currentUser,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun RewardCard(
    reward: RewardEntity,
    currentUser: UserEntity,
    viewModel: RewardViewModel
) {
    var isPaid by remember { mutableStateOf(reward.isPaid) }
    var showError by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface
        )
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
                        text = reward.monthYear,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // TODO: Fetch user name from userId
                    Text(
                        text = "Top Performer: User #${reward.userId}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Revenue: K${String.format("%,.2f", reward.totalRevenue)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Orders: ${reward.orderCount} | Voids: ${reward.voidCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Score: ${String.format("%.2f", reward.performanceScore)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                // Payout status indicator
                if (isPaid) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Paid",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Pending,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payout toggle (Owner only)
            if (currentUser.roleLevel == UserEntity.ROLE_OWNER && !isPaid) {
                Button(
                    onClick = {
                        viewModel.markAsPaid(
                            rewardId = reward.rewardId,
                            currentUser = currentUser,
                            onSuccess = { isPaid = true },
                            onError = { error -> showError = error }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Paid")
                }
            }
            
            if (isPaid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Payment Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Error message
            if (showError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = showError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun EmptyRewardsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Rewards Yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Top performers will appear here monthly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
