package fr.isen.amara.isensmartcompanion

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import fr.isen.amara.isensmartcompanion.screens.*

@Composable
fun AuthNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToResetPassword = { navController.navigate("reset") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("reset") {
            ResetPasswordScreen(
                onNavigateBackToLogin = { navController.navigate("login") }
            )
        }
        composable("main") {
            MainNavigation()
        }
    }
}
