package fr.isen.amara.isensmartcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import fr.isen.amara.isensmartcompanion.screens.AppGate
import fr.isen.amara.isensmartcompanion.ui.theme.ISENSmartCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ISENSmartCompanionTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    // Écoute l'état d'auth
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fb ->
            user = fb.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (user == null) {
        // Phase Auth: Login / Register / Reset
        AuthNavHost()
    } else {
        // Phase App: gate hauteur -> app
        AppGate {
            MainNavigation()
        }
    }
}
