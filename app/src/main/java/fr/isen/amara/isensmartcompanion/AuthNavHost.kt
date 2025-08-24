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
import fr.isen.amara.isensmartcompanion.screens.BinSetupScreen   // ✅ import du nouvel écran

@Composable
fun AuthNavHost() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "login",
        modifier = Modifier.fillMaxSize()
    ) {
        // --- Écran de connexion ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // 🔹 Après login, aller vers BinSetupScreen
                    nav.navigate("binSetup") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = { nav.navigate("register") },
                onNavigateToResetPassword = { nav.navigate("reset") }
            )
        }

        // --- Écran d'inscription ---
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    nav.popBackStack(route = "login", inclusive = false)
                },
                onNavigateToLogin = { nav.popBackStack() }
            )
        }

        // --- Reset mot de passe ---
        composable("reset") {
            ResetPasswordScreen(onNavigateBackToLogin = { nav.popBackStack() })
        }

        // --- Nouvel écran : setup // AuthNavHost.kt
        //package fr.isen.amara.isensmartcompanion
        //
        //import androidx.compose.foundation.layout.fillMaxSize
        //import androidx.compose.runtime.Composable
        //import androidx.compose.ui.Modifier
        //import androidx.navigation.compose.NavHost
        //import androidx.navigation.compose.composable
        //import androidx.navigation.compose.rememberNavController
        //import fr.isen.amara.isensmartcompanion.screens.LoginScreen
        //import fr.isen.amara.isensmartcompanion.screens.RegisterScreen
        //import fr.isen.amara.isensmartcompanion.screens.ResetPasswordScreen
        //
        //@Composable
        //fun AuthNavHost() {
        //    val nav = rememberNavController()
        //
        //    NavHost(
        //        navController = nav,
        //        startDestination = "login",
        //        modifier = Modifier.fillMaxSize()
        //    ) {
        //        composable("login") {
        //            LoginScreen(
        //                onLoginSuccess = {
        //                    // après login → aller vers l'app
        //                    nav.navigate("main") {
        //                        popUpTo("login") { inclusive = true }
        //                        launchSingleTop = true
        //                    }
        //                },
        //                onNavigateToRegister = { nav.navigate("register") },
        //                onNavigateToResetPassword = { nav.navigate("reset") }
        //            )
        //        }
        //
        //        composable("register") {
        //            RegisterScreen(
        //                onRegisterSuccess = {
        //                    nav.popBackStack(route = "login", inclusive = false)
        //                },
        //                onNavigateToLogin = { nav.popBackStack() }
        //            )
        //        }
        //
        //        composable("reset") {
        //            ResetPasswordScreen(onNavigateBackToLogin = { nav.popBackStack() })
        //        }
        //
        //        // Route "main" : on NE refait PAS le gate ici (il est déjà dans MainActivity)
        //        composable("main") { MainNavigation() }
        //    }
        //}de la hauteur ---
        composable("binSetup") {
            BinSetupScreen(nav)
        }

        // --- Application principale ---
        composable("main") {
            MainNavigation()
        }
    }
}
