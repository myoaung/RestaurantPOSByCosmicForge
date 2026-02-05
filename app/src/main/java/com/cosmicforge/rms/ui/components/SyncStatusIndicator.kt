package com.cosmicforge.rms.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow

/**
 * Sync Status Indicator - Shopify-style "Pill" UI
 * 
 * Stone Tier Requirement: "Does the Shopify-style indicator turn Red when the DeadLetterVault is full?"
 * 
 * Color Coding:
 * - Green: Synced (queue empty, no dead letters)
 * - Yellow: Syncing (queue has items)
 * - Red: Failed (dead letter vault has items requiring manager review)
 */
@Composable
fun SyncStatusIndicator(
    queueSize: Flow<Int>,
    deadLetterCount: Flow<Int>,
    modifier: Modifier = Modifier
) {
    val queueCount by queueSize.collectAsState(initial = 0)
    val deadCount by deadLetterCount.collectAsState(initial = 0)
    
    // Determine status
    val status = when {
        deadCount > 0 -> SyncIndicatorStatus.FAILED
        queueCount > 0 -> SyncIndicatorStatus.SYNCING
        else -> SyncIndicatorStatus.SYNCED
    }
    
    // Animated rotation for syncing icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = status.backgroundColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            when (status) {
                SyncIndicatorStatus.SYNCED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Synced",
                        tint = status.iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                SyncIndicatorStatus.SYNCING -> {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Syncing",
                        tint = status.iconColor,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(rotation)
                    )
                }
                SyncIndicatorStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        tint = status.iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Text
            Text(
                text = status.getLabel(queueCount, deadCount),
                color = status.textColor,
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium
            )
            
            // Badge for counts
            if (queueCount > 0 || deadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (deadCount > 0) Color(0xFFDC2626) else Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (deadCount > 0) "$deadCount" else "$queueCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sync indicator status enum
 */
private enum class SyncIndicatorStatus {
    SYNCED {
        override val backgroundColor = Color(0xFFDCFCE7) // Light green
        override val iconColor = Color(0xFF16A34A) // Green
        override val textColor = Color(0xFF166534) // Dark green
        override fun getLabel(queueCount: Int, deadCount: Int) = "Synced ✓"
    },
    SYNCING {
        override val backgroundColor = Color(0xFFFEF3C7) // Light yellow
        override val iconColor = Color(0xFFF59E0B) // Yellow/Amber
        override val textColor = Color(0xFF92400E) // Dark yellow
        override fun getLabel(queueCount: Int, deadCount: Int) = "Syncing..."
    },
    FAILED {
        override val backgroundColor = Color(0xFFFEE2E2) // Light red
        override val iconColor = Color(0xFFDC2626) // Red
        override val textColor = Color(0xFF991B1B) // Dark red
        override fun getLabel(queueCount: Int, deadCount: Int) = "Sync Failed"
    };
    
    abstract val backgroundColor: Color
    abstract val iconColor: Color
    abstract val textColor: Color
    abstract fun getLabel(queueCount: Int, deadCount: Int): String
}

/**
 * Compact version for toolbar/header
 */
@Composable
fun CompactSyncIndicator(
    queueSize: Flow<Int>,
    deadLetterCount: Flow<Int>,
    modifier: Modifier = Modifier
) {
    val queueCount by queueSize.collectAsState(initial = 0)
    val deadCount by deadLetterCount.collectAsState(initial = 0)
    
    val status = when {
        deadCount > 0 -> SyncIndicatorStatus.FAILED
        queueCount > 0 -> SyncIndicatorStatus.SYNCING
        else -> SyncIndicatorStatus.SYNCED
    }
    
    Box(
        modifier = modifier
            .size(12.dp)
            .background(status.iconColor, CircleShape)
    )
}
