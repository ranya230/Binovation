// MainActivity.kt
package fr.isen.amara.isensmartcompanion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import fr.isen.amara.isensmartcompanion.screens.AppGate
import fr.isen.amara.isensmartcompanion.ui.theme.ISENSmartCompanionTheme
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            ISENSmartCompanionTheme {
                AppRoot() // on garde tout pareil derrière le splash
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }
}

@Composable
private fun AppRoot() {
    // --- Splash state ---
    var showSplash by remember { mutableStateOf(true) }

    // --- Splash temporisation ---
    LaunchedEffect(Unit) {
        // durée du splash (ajuste si tu veux)
        delay(2000)
        showSplash = false
    }

    // --- Splash ---
    AnimatedSplash(visible = showSplash)

    // --- App (auth / gate) après splash ---
    if (!showSplash) {
        val auth = remember { FirebaseAuth.getInstance() }
        var user by remember { mutableStateOf(auth.currentUser) }

        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { fb ->
                user = fb.currentUser
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }

        if (user == null) {
            // Phase Auth: Login / Register / Reset
            AuthNavHost() // -> navigue vers "main" après login
        } else {
            // Phase App: GATE hauteur -> app
            AppGate {
                MainNavigation() // nav principale (home/assistant/settings/…)
            }
        }
    }
}

/* ----------------- SPLASH IMPRESSIONNANT ----------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedSplash(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            // part du bas
            initialOffsetY = { it / 6 },
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
            // sort vers le haut
            targetOffsetY = { -it / 6 },
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    ) {
        SplashScreen()
    }
}

@Composable
private fun SplashScreen() {
    // animation de scale + fade progressive sur les logos
    var start by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (start) 1f else 0.85f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 250),
        label = "titleAlpha"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 450),
        label = "subtitleAlpha"
    )

    LaunchedEffect(Unit) { start = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.isen),
                    contentDescription = "ISEN",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                )
                Image(
                    painter = painterResource(id = R.drawable.binovation_logo),
                    contentDescription = "Binovation",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Welcome to Binovation",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.alpha(titleAlpha)
            )
            Text(
                text = "Smart Waste Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }
    }
}
