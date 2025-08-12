package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.amara.isensmartcompanion.R
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
fun BinStateScreen() {
    val context = LocalContext.current

    // 1) Un seul client MQTT pour cet écran (même pattern que BinLevel)
    val mqtt = remember { MqttClientHelper(context) }

    // 2) États UI
    var distance by remember { mutableStateOf(30f) }
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 30f
    val fillPercentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)

    val history = remember { mutableStateListOf<Float>() }
    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    // 3) Connexion + abonnement + cleanup (identique à BinLevel)
    DisposableEffect(Unit) {
        mqtt.connect()
        mqtt.subscribe("Distance") { payload ->
            parseDistance(payload)?.let { newDistance ->
                distance = newDistance

                val percent = ((maxDistance - newDistance) / maxDistance * 100f)
                    .coerceIn(0f, 100f)

                if (history.size >= 10) history.removeAt(0)
                history.add(percent)

                lastUpdateTime = System.currentTimeMillis()
            }
        }
        onDispose {
            mqtt.unsubscribe("Distance")
            mqtt.disconnect()
        }
    }

    // 4) Petite analyse simple
    val trend = remember(history.toList()) {
        if (history.size >= 3) {
            val (a, b, c) = history.takeLast(3)
            when {
                c > b && b > a -> "Increasing"
                c < b && b < a -> "Decreasing"
                else -> "Stable"
            }
        } else null
    }

    val secondsSinceUpdate = lastUpdateTime?.let { ((System.currentTimeMillis() - it) / 1000).toInt() }

    val estimatedTimeHours = if (history.size >= 2) {
        val delta = history.last() - history.first()
        if (delta <= 0.5f) null
        else {
            // approx : 1 point ≈ 1 minute d'intervalle si tes envois sont réguliers
            val ratePerMin = delta / history.size
            val remaining = 100f - fillPercentage
            (remaining / ratePerMin / 60).coerceAtMost(24f)
        }
    } else null

    // 5) UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bin State",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Trash Bin",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Fill Level", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text("${fillPercentage.roundToInt()}%", fontSize = 36.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        if (trend != null || estimatedTimeHours != null || secondsSinceUpdate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("System Analysis", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    secondsSinceUpdate?.let { if (it > 0) Text("Last update: ${it} seconds ago") }
                    trend?.let { Text("Trend: $it") }
                    estimatedTimeHours?.let { Text("Est. time before full: ${it.roundToInt()} hours") }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (history.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last Readings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        itemsIndexed(history.asReversed()) { index, value ->
                            Text("Reading ${history.size - index}: ${value.roundToInt()}%")
                        }
                    }
                }
            }
        }
    }
}

/** Parseur identique à BinLevel : JSON {"distance": 1234}, brut "1234", texte "Distance: 1234 mm" */
private fun parseDistance(message: String): Float? {
    // JSON
    try {
        val json = JSONObject(message)
        if (json.has("distance")) return json.getDouble("distance").toFloat()
    } catch (_: Exception) { /* pas du JSON */ }

    // Nombre brut
    message.toFloatOrNull()?.let { return it }

    // Texte libre -> premier nombre
    return Regex("""-?\d+(\.\d+)?""").find(message)?.value?.toFloatOrNull()
}
