package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

@Composable
fun TrashStatusScreen() {
    val mqttClient = remember { MqttClientHelper() }
    val coroutineScope = rememberCoroutineScope()
    var distance by remember { mutableStateOf(30f) }
    val fillPercentage = ((30f - distance) / 30f * 100f).coerceIn(0f, 100f)

    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("smartbin/status") { message ->
            try {
                val json = JSONObject(message)
                val newDistance = json.getDouble("distance").toFloat()
                distance = newDistance
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Trash Bin",
            modifier = Modifier.size(160.dp)
        )

        Text(
            text = "Fill Level: ${fillPercentage.toInt()}%",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        LinearProgressIndicator(
            progress = fillPercentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    mqttClient.publish("smartbin/scan", "start")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Trash Level")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuButton("Analyse", Modifier.weight(1f).padding(horizontal = 4.dp))
            MenuButton("Historique", Modifier.weight(1f).padding(horizontal = 4.dp))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            MenuButton("Bin State", Modifier.weight(1f).padding(horizontal = 4.dp))
            MenuButton("Bin History", Modifier.weight(1f).padding(horizontal = 4.dp))
        }
    }
}

@Composable
fun MenuButton(title: String, modifier: Modifier = Modifier) {
    Button(
        onClick = { /* Navigation à ajouter */ },
        modifier = modifier
    ) {
        Text(title)
    }
}
