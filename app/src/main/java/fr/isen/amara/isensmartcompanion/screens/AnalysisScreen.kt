package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun AnalysisScreen() {
    val mqttClient = remember { MqttClientHelper() }
    val coroutineScope = rememberCoroutineScope()
    var distance by remember { mutableStateOf(30f) }
    val fillPercent = ((30f - distance) / 30f * 100f).coerceIn(0f, 100f).roundToInt()

    var previousDistance by remember { mutableStateOf(30f) }
    var fillRatePerDay by remember { mutableStateOf(5) }
    val estimatedDaysLeft = ((100 - fillPercent) / fillRatePerDay.toFloat()).coerceAtLeast(0f).roundToInt()

    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("smartbin/status") { message ->
            try {
                val json = JSONObject(message)
                val newDist = json.getDouble("distance").toFloat()
                if (newDist != distance) {
                    previousDistance = distance
                    distance = newDist
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Smart Bin Analysis",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        LinearProgressIndicator(
            progress = fillPercent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = Color(0xFF444444)
        )

        Text(
            "Current Fill Level: $fillPercent%",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Divider(thickness = 1.dp)

        Text("Estimated Daily Fill Rate: $fillRatePerDay%", fontSize = 16.sp)
        Text("Estimated Days Until Full: $estimatedDaysLeft days", fontSize = 16.sp)

        Divider(thickness = 1.dp)

        Text("Last Measured Distance: ${distance} cm", fontSize = 16.sp, color = Color.Gray)
        Text("Previous Distance: $previousDistance cm", fontSize = 16.sp, color = Color.Gray)

        Divider(thickness = 1.dp)

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    mqttClient.publish("smartbin/scan", "start")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-scan Now")
        }
    }
}
