package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.navigation.NavController
import fr.isen.amara.isensmartcompanion.R

// Fonction utilitaire : enregistrer la distance max
fun saveMaxDistance(context: Context, value: Float) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs.edit().putFloat("max_distance", value).apply()
}

// Fonction utilitaire : récupérer la distance max
fun getMaxDistance(context: Context): Float {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return prefs.getFloat("max_distance", 30f)
}

@Composable
fun TrashStatusScreen(navController: NavController) {
    val context = LocalContext.current
    val mqttClient = remember { MqttClientHelper() }
    val coroutineScope = rememberCoroutineScope()

    var distance by remember { mutableStateOf(30f) }
    var maxDistance by remember { mutableStateOf(getMaxDistance(context)) }
    var scanStatus by remember { mutableStateOf("") }

    val fillPercentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)
    var notificationSent by remember { mutableStateOf(false) }

    // Réception MQTT
    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("Distance") { message ->
            try {
                val json = JSONObject(message)
                val newDistance = json.getDouble("distance").toFloat()
                distance = newDistance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Notification si la poubelle est presque pleine
    LaunchedEffect(fillPercentage) {
        if (fillPercentage >= 95 && !notificationSent) {
            showFullNotification(context)
            notificationSent = true
        }
        if (fillPercentage < 90) {
            notificationSent = false
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Trash Bin",
            modifier = Modifier.size(160.dp)
        )

        Text(
            text = "Current fill level: ${fillPercentage.toInt()}%",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        LinearProgressIndicator(
            progress = fillPercentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // ✅ Bouton pour lancer un scan MQTT
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    mqttClient.publish("smartbin/scan", "start")
                    scanStatus = "Scan sent!"
                    delay(3000)
                    scanStatus = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Now")
        }

        // ✅ Message temporaire de confirmation
        if (scanStatus.isNotEmpty()) {
            Text(
                text = scanStatus,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        // ✅ Boutons de navigation
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuButton("Bin Level", Modifier.weight(1f)) { navController.navigate("binLevel") }
            MenuButton("Bin State", Modifier.weight(1f)) { navController.navigate("binState") }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuButton("Historique", Modifier.weight(1f)) { navController.navigate("history") }
            MenuButton("Analysis", Modifier.weight(1f)) { navController.navigate("analysis") }
        }
    }
}

// Composable pour les boutons du menu
@Composable
fun MenuButton(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(title)
    }
}
