@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cosmicforge.rms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.cosmicforge.rms.data.database.DatabaseSeeder
import com.cosmicforge.rms.data.database.entities.UserEntity
import com.cosmicforge.rms.ui.MainDashboardScreen
import com.cosmicforge.rms.ui.auth.LoginScreen
import com.cosmicforge.rms.ui.theme.CosmicForgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for Cosmic Forge RMS
 * Phase 2: Visibility Unlock with ADMIN test user
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
                    // Phase 2: Direct launch with test ADMIN user
                    var currentUser by remember { 
                        mutableStateOf<UserEntity?>(
                            // Test ADMIN user for visibility unlock
                            UserEntity(
                                userId = 1L,
                                userName = "Admin",
                                pinHash = "",
                                roleLevel = UserEntity.ROLE_OWNER,
                                isActive = true
                            )
                        ) 
                    }
                    
                    if (currentUser != null) {
                        MainDashboardScreen(
                            currentUser = currentUser!!,
                            onLogout = { currentUser = null }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = { user -> currentUser = user }
                        )
                    }
                }
            }
        }
    }
}
