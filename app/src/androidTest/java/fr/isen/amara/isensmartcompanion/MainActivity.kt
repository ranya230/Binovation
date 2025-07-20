package fr.isen.amara.isensmartcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import fr.isen.amara.isensmartcompanion.data.InteractionViewModel
import fr.isen.amara.isensmartcompanion.navigation.BottomNavItem
import fr.isen.amara.isensmartcompanion.screens.*
import fr.isen.amara.isensmartcompanion.ui.theme.ISENSmartCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ISENSmartCompanionTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != "login") {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavigationGraph(navController, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Events,
        BottomNavItem.Agenda,
        BottomNavItem.History
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = false,
                onClick = { navController.navigate(item.route) }
            )
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val eventsViewModel: EventsViewModel = viewModel()
    val interactionViewModel: InteractionViewModel = viewModel()

    NavHost(navController, startDestination = "login", modifier = modifier) {
        // Écran de connexion
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(BottomNavItem.Home.route) {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onForgotPassword = {
                    // À compléter si tu veux gérer le reset de mot de passe
                }
            )
        }

        // Autres écrans
        composable(BottomNavItem.Home.route) {
            AssistantScreen()
        }

        composable(BottomNavItem.Events.route) {
            EventsScreen(navController, eventsViewModel)
        }

        composable(BottomNavItem.Agenda.route) {
            AgendaScreen(navController)
        }

        composable(BottomNavItem.History.route) {
            HistoryScreen(interactionViewModel, navController)
        }

        composable("eventDetail/{eventId}") { backStackEntry ->
            EventDetailScreen(navController, backStackEntry, eventsViewModel)
        }

        composable("historyDetail/{interactionId}") { backStackEntry ->
            HistoryDetailScreen(navController, backStackEntry, interactionViewModel)
        }
    }
}
