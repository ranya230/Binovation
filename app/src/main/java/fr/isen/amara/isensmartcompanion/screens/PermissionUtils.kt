package fr.isen.amara.isensmartcompanion.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Composable qui demande la permission POST_NOTIFICATIONS (Android 13+)
 */
@Composable
fun RequestNotificationPermission() {
    // Lanceur pour demander la permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* permission accordée ou refusée, ici on ne fait rien de spécial */ }
    )

    // Demande la permission uniquement si API >= 33
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
