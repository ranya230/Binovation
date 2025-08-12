package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun AnalysisScreen() {
    val context = LocalContext.current

    // 1) Un seul client MQTT pour cet écran
    val mqtt = remember { MqttClientHelper(context) }

    // 2) États
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 30f
    val history = remember { mutableStateListOf<Pair<Float, Long>>() }
    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    // Optionnel: pour limiter à 1 point / 20s max (si tes messages arrivent plus souvent)
    var lastAcceptedTs by remember { mutableStateOf(0L) }
    val periodMs = 20_000L  // 20 secondes

    // 3) Connexion + abonnement, puis cleanup
    DisposableEffect(Unit) {
        mqtt.connect()
        mqtt.subscribe("Distance") { payload ->
            val dist = parseDistance(payload) ?: return@subscribe
            val now = System.currentTimeMillis()

            // (Optionnel) N’enregistrer qu’un point toutes les 20s
            if (now - lastAcceptedTs < periodMs) return@subscribe
            lastAcceptedTs = now

            val percent = ((maxDistance - dist) / maxDistance * 100f).coerceIn(0f, 100f)
            history.add(percent to now)
            if (history.size > 200) history.removeAt(0) // petit tampon
            lastUpdateTime = now
        }
        onDispose {
            mqtt.unsubscribe("Distance")
            mqtt.disconnect()
        }
    }

    // 4) Petites analyses
    val now = System.currentTimeMillis()
    val oneHourMs = 60 * 60 * 1000

    val usagePattern = remember(history) {
        val lastDay = history.filter { now - it.second <= 24 * oneHourMs }
        val mostUsedHour = lastDay
            .groupBy { Calendar.getInstance().apply { timeInMillis = it.second }.get(Calendar.HOUR_OF_DAY) }
            .maxByOrNull { it.value.size }?.key

        val averageDeltaSec = if (lastDay.size >= 2) {
            val deltas = lastDay.zipWithNext { a, b -> b.second - a.second }
            (deltas.map { it / 1000 }.average()).toInt()
        } else null

        mostUsedHour to averageDeltaSec
    }

    val fillPercentage = history.lastOrNull()?.first?.roundToInt() ?: 0
    val estFullTime: String? = if (history.size >= 3) {
        val delta = history.last().first - history.first().first
        val timeElapsedSec = (history.last().second - history.first().second) / 1000
        if (delta > 5f && timeElapsedSec > 0) {
            val ratePerSecond = delta / timeElapsedSec
            val remaining = 100f - history.last().first
            if (ratePerSecond > 0f) {
                val secondsToFull = (remaining / ratePerSecond).toLong()
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now + secondsToFull * 1000))
            } else null
        } else null
    } else null

    // 5) UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Smart Bin Analysis", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Current Fill Level: $fillPercentage%", fontSize = 18.sp)
                estFullTime?.let {
                    Text("Estimated full at: $it", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF78E6FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Usage Pattern", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                usagePattern.first?.let { Text("Most active hour: ${it}h") }
                usagePattern.second?.let { Text("Avg. interval between updates: ${it} sec") }
            }
        }

        lastUpdateTime?.let { ts ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Text("Last update: ${sdf.format(Date(ts))}", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

/** Parse robuste du message MQTT : JSON {"distance": 1234}, nombre brut "1234", ou texte libre "Distance: 1234 mm" */
private fun parseDistance(message: String): Float? {
    // JSON
    try {
        val json = org.json.JSONObject(message)
        if (json.has("distance")) {
            val any = json.get("distance")
            return when (any) {
                is Number -> any.toFloat()
                is String -> any.toFloatOrNull()
                else -> null
            }
        }
    } catch (_: Exception) { /* pas du JSON */ }

    // Nombre brut
    message.toFloatOrNull()?.let { return it }

    // Texte libre -> premier nombre
    val regex = Regex("""-?\d+(\.\d+)?""")
    return regex.find(message)?.value?.toFloatOrNull()
}