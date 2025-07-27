package fr.isen.amara.isensmartcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.isen.amara.isensmartcompanion.screens.LoginScreen
import fr.isen.amara.isensmartcompanion.screens.RegisterScreen
import fr.isen.amara.isensmartcompanion.ui.theme.ISENSmartCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ISENSmartCompanionTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(navController)
                        }
                        composable("register") {
                            RegisterScreen(navController)
                        }
                        composable("home") {
                            // Page d'accueil (à créer selon ton app)
                            Surface {
                                androidx.compose.material3.Text("Bienvenue ! (Home screen)")
                            }
                        }
                    }
                }
            }
        }
    }
}
