package com.cosmicforge.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.cosmicforge.pos.ui.navigation.NavGraph
import com.cosmicforge.pos.ui.theme.CosmicForgeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Cosmic Forge POS - Phase 2 Transition
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CosmicForgeTheme {
                // Initialize the NavController to manage screen transitions
                val navController = rememberNavController()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // NavGraph now controls which screen is shown
                    NavGraph(navController = navController)
                }
            }
        }
    }
}

/**
 * Updated WelcomeScreen with Navigation trigger
 */
@Composable
fun WelcomeScreen(onNavigateToLogin: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🚀 Cosmic Forge POS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Phase 1: Foundation Complete",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "✓ Database Schema Ready\n✓ Hilt DI Configured\n✓ SQLCipher Encryption Enabled",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // NEW: Button to move to Phase 2 (Login)
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Get Started")
            }
        }
    }
}