package fr.isen.amara.isensmartcompanion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.isen.amara.isensmartcompanion.screens.LoginScreen
import fr.isen.amara.isensmartcompanion.screens.RegisterScreen
import fr.isen.amara.isensmartcompanion.screens.ResetPasswordScreen

@Composable
fun AuthNavHost() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "login",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Après connexion, aller vers l'app principale et retirer "login" de la back stack
                    nav.navigate("main") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = { nav.navigate("register") },
                onNavigateToResetPassword = { nav.navigate("reset") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    // Une fois inscrit, revenir sur l'écran de login
                    nav.popBackStack(route = "login", inclusive = false)
                },
                onNavigateToLogin = {
                    // Retour simple vers login
                    nav.popBackStack()
                }
            )
        }

        composable("reset") {
            ResetPasswordScreen(
                onNavigateBackToLogin = {
                    nav.popBackStack()
                }
            )
        }

        // Route vers ton app principale
        composable("main") {
            MainNavigation()
        }
    }
}
