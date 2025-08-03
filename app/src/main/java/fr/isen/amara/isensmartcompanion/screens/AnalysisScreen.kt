package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.json.JSONObject
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalysisScreen() {
    val context = LocalContext.current
    val mqttClient = remember { MqttClientHelper() }
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 30f
    val history = remember { mutableStateListOf<Pair<Float, Long>>() }

    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("Distance") { message ->
            try {
                val json = JSONObject(message)
                val distance = json.getDouble("distance").toFloat()
                val percent = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)
                val timestamp = System.currentTimeMillis()
                history.add(percent to timestamp)
                if (history.size > 100) history.removeAt(0)
                lastUpdateTime = timestamp
            } catch (_: Exception) {}
        }
    }

    val now = System.currentTimeMillis()
    val oneHourMs = 60 * 60 * 1000

    val usagePattern = remember(history) {
        val lastDay = history.filter { now - it.second <= 24 * oneHourMs }
        val mostUsedHour = lastDay.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.second }.get(Calendar.HOUR_OF_DAY)
        }.maxByOrNull { it.value.size }?.key

        val averageDelta = if (lastDay.size >= 2) {
            val deltas = lastDay.zipWithNext { a, b -> b.second - a.second }
            deltas.map { it / 1000 }.average().toInt()
        } else null

        Pair(mostUsedHour, averageDelta)
    }

    val fillPercentage = history.lastOrNull()?.first?.roundToInt() ?: 0
    val estFullTime = if (history.size >= 3) {
        val delta = history.last().first - history.first().first
        val timeElapsed = (history.last().second - history.first().second) / 1000
        if (delta > 5) {
            val ratePerSecond = delta / timeElapsed
            val remaining = 100 - history.last().first
            val secondsToFull = (remaining / ratePerSecond).toLong()
            SimpleDateFormat("HH:mm").format(Date(now + secondsToFull * 1000))
        } else null
    } else null

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
                if (estFullTime != null)
                    Text("Estimated full at: $estFullTime", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF78E6FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Usage Pattern", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                if (usagePattern.first != null)
                    Text("Most active hour: ${usagePattern.first}h")
                if (usagePattern.second != null)
                    Text("Avg. interval between updates: ${usagePattern.second} sec")
            }
        }

        if (lastUpdateTime != null) {
            val last = Date(lastUpdateTime!!)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            Text("Last update: ${sdf.format(last)}", fontSize = 13.sp, color = Color.Gray)
        }
    }
}
