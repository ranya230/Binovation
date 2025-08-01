package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashStatusScreen() {
    val mqttClient = remember { MqttClientHelper() }
    val coroutineScope = rememberCoroutineScope()

    var distance by remember { mutableStateOf(30f) } // valeur max par défaut
    val fillPercentage = ((30f - distance) / 30f * 100f).coerceIn(0f, 100f)

    LaunchedEffect(Unit) {
        mqttClient.connectAndSubscribe("smartbin/status") { message ->
            try {
                val json = JSONObject(message)
                val newDistance = json.getDouble("distance").toFloat()
                distance = newDistance
            } catch (e: Exception) {
                println("Erreur JSON: ${e.localizedMessage}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accueil") },
                actions = {
                    IconButton(onClick = { /* Naviguer vers le profil */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil")
                    }
                    IconButton(onClick = { /* Naviguer vers l'assistance AI */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Assistance")
                    }
                    IconButton(onClick = { /* Naviguer vers les paramètres */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Paramètres")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.poubelle), // mets ici ton image
                contentDescription = "Trash Bin",
                modifier = Modifier.size(150.dp)
            )

            Text(
                text = "Remplissage: ${fillPercentage.toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                Text("Lancer le Scan")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuButton("Analyse", Modifier.weight(1f).padding(horizontal = 4.dp))
                MenuButton("Historique", Modifier.weight(1f).padding(horizontal = 4.dp))
                MenuButton("Bin State", Modifier.weight(1f).padding(horizontal = 4.dp))
                MenuButton("Bin History", Modifier.weight(1f).padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
fun MenuButton(title: String, modifier: Modifier = Modifier) {
    Button(
        onClick = { /* ajouter navigation ici si besoin */ },
        modifier = modifier
    ) {
        Text(title)
    }
}
