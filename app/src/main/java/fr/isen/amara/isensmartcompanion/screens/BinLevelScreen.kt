package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import fr.isen.amara.isensmartcompanion.R
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
fun BinLevelScreen() {
    val context = LocalContext.current

    // 1) Un seul client MQTT pour cet écran (le helper attend un Context)
    val mqtt = remember { MqttClientHelper(context) }

    // 2) États UI
    var distance by remember { mutableStateOf(30f) }
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 30f
    val fillPercentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)

    // 3) Connexion + abonnement au topic "Distance", puis nettoyage à la fermeture
    DisposableEffect(Unit) {
        mqtt.connect()
        mqtt.subscribe("Distance") { payload ->
            // Formats acceptés : {"distance":1234}, "1234", "Distance: 1234"
            parseDistance(payload)?.let { newValue ->
                distance = newValue
            }
        }
        onDispose {
            mqtt.unsubscribe("Distance")
            mqtt.disconnect()
        }
    }

    // 4) Couleur/texte d’état selon le pourcentage
    val statusColor = when {
        fillPercentage >= 95f -> Color(0xFFC62828)
        fillPercentage >= 80f -> Color(0xFFFF8F00)
        fillPercentage >= 50f -> Color(0xFFFFD600)
        else -> Color(0xFF43A047)
    }
    val statusText = if (fillPercentage >= 95f) "The bin must be emptied." else ""

    // 5) UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Bin Level",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Trash Bin",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = "Fill Level",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${fillPercentage.roundToInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                color = statusColor,
                shape = RoundedCornerShape(6.dp)
            ) {}
        }

        LinearProgressIndicator(
            progress = fillPercentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = statusColor
        )

        if (statusText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFC62828),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Parse robuste du message MQTT : JSON {"distance":1234}, brut "1234", texte "Distance: 1234 mm" */
private fun parseDistance(message: String): Float? {
    // JSON
    try {
        val json = JSONObject(message)
        if (json.has("distance")) return json.getDouble("distance").toFloat()
    } catch (_: Exception) { /* pas du JSON */ }

    // Nombre brut
    message.toFloatOrNull()?.let { return it }

    // Texte libre → premier nombre
    val regex = Regex("""-?\d+(\.\d+)?""")
    return regex.find(message)?.value?.toFloatOrNull()
}
