package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
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
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun BinLevelScreen() {
    val context = LocalContext.current
    val mqttClient = remember { MqttClientHelper() }

    var distance by remember { mutableStateOf(30f) }
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 30f
    val fillPercentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)

    // MQTT Reception
    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("Distance") { message ->
            val json = JSONObject(message)
            val newDistance = json.getDouble("distance").toFloat()
            distance = newDistance
        }
    }

    val statusColor = when {
        fillPercentage >= 95f -> Color(0xFFC62828) // Red
        fillPercentage >= 80f -> Color(0xFFFF8F00) // Orange
        fillPercentage >= 50f -> Color(0xFFFFD600) // Yellow
        else -> Color(0xFF43A047)                // Green
    }

    val statusText = if (fillPercentage >= 95f) "The bin must be emptied." else ""

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ✅ Titre principal
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

        // Pourcentage + couleur
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

        // ProgressBar avec la couleur dynamique
        LinearProgressIndicator(
            progress = fillPercentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = statusColor
        )

        // Message d’alerte si nécessaire
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
