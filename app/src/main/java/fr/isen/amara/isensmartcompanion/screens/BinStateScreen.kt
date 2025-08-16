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

@Composable
fun BinStateScreen() {
    val context = LocalContext.current
    val mqtt = remember { MqttClientHelper(context) }

    var distance by remember { mutableStateOf(1200f) }
    val userMax = getMaxDistance(context)
    val maxDistance = if (userMax > 0) userMax else 1200f
    val fill = computeFillPercent(distance, maxDistance)

    val history = remember { mutableStateListOf<Float>() }
    var lastUpdate by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        mqtt.connectAndSubscribe("Distance") { payload ->
            parseDistance(payload)?.let { d ->
                distance = d
                val p = computeFillPercent(d, maxDistance)
                if (history.size >= 10) history.removeAt(0)
                history.add(p)
                lastUpdate = System.currentTimeMillis()
            }
        }
        onDispose {
            mqtt.unsubscribe("Distance")
            mqtt.disconnect()
        }
    }

    val trend = remember(history.toList()) {
        if (history.size >= 3) {
            val (a, b, c) = history.takeLast(3)
            when {
                c > b && b > a -> "Increasing"
                c < b && b < a -> "Decreasing"
                else           -> "Stable"
            }
        } else null
    }
    val secondsSince = lastUpdate?.let { ((System.currentTimeMillis() - it) / 1000).toInt() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bin State", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Image(painterResource(R.drawable.poubelle), contentDescription = null, modifier = Modifier.size(200.dp))
        Spacer(Modifier.height(8.dp))
        Text("Fill Level", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text("${fill.roundToInt()}%", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        if (trend != null || secondsSince != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("System Analysis", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    secondsSince?.let { if (it > 0) Text("Last update: ${it}s ago") }
                    trend?.let { Text("Trend: $it") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (history.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last Readings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
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
