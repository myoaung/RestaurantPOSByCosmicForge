package com.cosmicforge.pos

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.cosmicforge.pos.data.database.DatabaseSeeder
import com.cosmicforge.pos.data.database.entities.UserEntity
import com.cosmicforge.pos.ui.MainDashboardScreen
import com.cosmicforge.pos.ui.auth.LoginScreen
import com.cosmicforge.pos.ui.theme.CosmicForgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for Cosmic Forge POS
 * Phase 3: Core POS Implementation
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
                var currentUser by remember { mutableStateOf<UserEntity?>(null) }
                
                if (currentUser == null) {
                    // Show login screen
                    LoginScreen(
                        onLoginSuccess = { user ->
                            currentUser = user
                        }
                    )
                } else {
                    // Show main dashboard
                    MainDashboardScreen(
                        currentUser = currentUser!!,
                        onLogout = {
                            currentUser = null
                        }
                    )
                }
            }
        }
    }
}
