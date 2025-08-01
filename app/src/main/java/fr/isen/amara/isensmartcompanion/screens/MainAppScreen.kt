package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import fr.isen.amara.isensmartcompanion.screens.*

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf("home", "assistant", "settings")

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            when (screen) {
                                "home" -> Icon(Icons.Default.Home, contentDescription = null)
                                "assistant" -> Icon(Icons.Default.SmartToy, contentDescription = null)
                                "settings" -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(screen.capitalize()) },
                        selected = navController.currentBackStackEntryAsState().value?.destination?.route == screen,
                        onClick = {
                            navController.navigate(screen) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { TrashStatusScreen() }
            composable("assistant") { AssistantScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}