package com.cosmicforge.pos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmicforge.pos.WelcomeScreen // Import from MainActivity
import com.cosmicforge.pos.ui.auth.LoginScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // 1. The Welcome/Splash Screen
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { 
                    navController.navigate("login") 
                }
            )
        }

        // 2. The Login Screen (The one we fixed earlier!)
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Once logged in, move to dashboard and clear the "Welcome" history
                    navController.navigate("dashboard") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
        // 3. The Dashboard Placeholder (To prevent crashes on login success)
        composable("dashboard") {
            // You can replace this with your actual DashboardScreen later
            androidx.compose.material3.Text("Welcome to the Dashboard!")
        }
    }
}