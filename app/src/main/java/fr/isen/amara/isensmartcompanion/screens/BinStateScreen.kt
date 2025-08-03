package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import fr.isen.amara.isensmartcompanion.R
import kotlin.math.roundToInt
@Composable
fun BinStateScreen() {
    val context = LocalContext.current
    val mqttClient = remember { MqttClientHelper() }

    var distance by remember { mutableStateOf(30f) }
    val maxDistance = getMaxDistance(context)
    val fillPercentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)

    val history = remember { mutableStateListOf<Float>() }
    var lastUpdateTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("Distance") { message ->
            try {
                val json = JSONObject(message)
                val newDistance = json.getDouble("distance").toFloat()
                distance = newDistance

                val percent = ((maxDistance - newDistance) / maxDistance * 100f).coerceIn(0f, 100f)
                if (history.size >= 10) history.removeAt(0)
                history.add(percent)

                lastUpdateTime = System.currentTimeMillis()
            } catch (_: Exception) {}
        }
    }

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

    val secondsSinceUpdate = lastUpdateTime?.let {
        ((System.currentTimeMillis() - it) / 1000).toInt()
    }

    val estimatedTimeHours = if (history.size >= 2) {
        val delta = history.last() - history.first()
        if (delta <= 0.5f) null
        else {
            val ratePerMin = delta / (history.size * 1)
            val remaining = 100f - fillPercentage
            (remaining / ratePerMin / 60).coerceAtMost(24f)
        }
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Titre principal
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

        Text(
            text = "Fill Level",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${fillPercentage.roundToInt()}%",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (trend != null || estimatedTimeHours != null || secondsSinceUpdate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Analysis", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (secondsSinceUpdate != null && secondsSinceUpdate > 0)
                        Text("Last update: $secondsSinceUpdate seconds ago")
                    if (trend != null)
                        Text("Trend: $trend")
                    if (estimatedTimeHours != null)
                        Text("Est. time before full: ${estimatedTimeHours.roundToInt()} hours")
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Readings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        itemsIndexed(history.reversed(), key = { index, _ -> index }) { index: Int, value: Float ->
                            Text("Reading ${history.size - index}: ${value.roundToInt()}%")
                        }
                    }
                }
            }
        }
    }
}
