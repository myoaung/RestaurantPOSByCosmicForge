package com.cosmicforge.rms.ui.navigation

// DEPRECATED: This NavGraph is no longer used after Phase 2 refactor
// MainActivity now directly launches MainDashboardScreen with test ADMIN user
// Keeping this file for reference only

/*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmicforge.rms.ui.auth.LoginScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // 1. The Welcome/Splash Screen
        composable("welcome") {
            // WelcomeScreen removed - now using direct MainDashboardScreen launch
        }

        // 2. The Login Screen
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
        // 3. The Dashboard Placeholder
        composable("dashboard") {
            androidx.compose.material3.Text("Welcome to the Dashboard!")
        }
    }
}
*/
