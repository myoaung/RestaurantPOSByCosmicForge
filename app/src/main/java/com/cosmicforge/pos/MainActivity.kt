package com.cosmicforge.pos

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import com.cosmicforge.pos.ui.theme.CosmicForgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for Cosmic Forge POS
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Seed mock data on first launch
        lifecycleScope.launch {
            databaseSeeder.seedMockData()
        }
        
        setContent {
            CosmicForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Placeholder for Phase 1
                    WelcomeScreen()
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
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
        }
    }
}
