package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.navigation.NavController
import fr.isen.amara.isensmartcompanion.R

/* ====== Utils: sauvegarde / lecture hauteur max ====== */
fun saveMaxDistance(context: Context, value: Float) {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    prefs.edit().putFloat("max_distance", value).apply()
}

fun getMaxDistance(context: Context): Float {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return prefs.getFloat("max_distance", 1200f)
}

/* ====== Parsing robuste du payload MQTT ====== */
private fun parseDistanceSafe(message: String): Float? {
    // 1) JSON {"distance": 1200} ou {"Distance": 1200}
    try {
        val json = JSONObject(message)
        if (json.has("distance")) {
            val v = json.get("distance")
            return (v as? Number)?.toFloat() ?: (v as? String)?.toFloatOrNull()
        }
        if (json.has("Distance")) {
            val v = json.get("Distance")
            return (v as? Number)?.toFloat() ?: (v as? String)?.toFloatOrNull()
        }
    } catch (_: Exception) { /* pas JSON objet */ }

    // 2) nombre brut "1200"
    message.toFloatOrNull()?.let { return it }

    // 3) "Distance: 1200" ou similaire
    val regex = Regex("""-?\d+(\.\d+)?""")
    return regex.find(message)?.value?.toFloatOrNull()
}

/* ====== Écran principal ====== */
@Composable
fun TrashStatusScreen(navController: NavController) {
    val context = LocalContext.current
    val mqttClient = remember { MqttClientHelper(context) }
    val scope = rememberCoroutineScope()

    // Paramètres
    val inactivityMs = 12_000L // revenir à 0% si aucune donnée pendant 12 s
    val maxDistancePref = remember { getMaxDistance(context) }

    // Etat "pas de donnée encore" -> on force 0% tant qu'aucune lecture valide
    var hasReading by remember { mutableStateOf(false) }

    // Distance courante; on initialise à maxDistance pour que le calcul donne 0 %
    var distance by remember { mutableStateOf(maxDistancePref) }

    // On garde maxDistance séparé si tu le changes un jour dans Settings
    var maxDistance by remember { mutableStateOf(maxDistancePref) }

    var scanStatus by remember { mutableStateOf("") }
    var notificationSent by remember { mutableStateOf(false) }
    var lastMsgTs by remember { mutableStateOf(0L) }

    // Pour ignorer un éventuel message "retained" au 1er abonnement
    var firstMessageIgnored by remember { mutableStateOf(false) }

    val fillPercentage = if (hasReading) {
        ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)
    } else {
        0f
    }

    /* Connexion / abonnement MQTT + cleanup */
    DisposableEffect(Unit) {
        // 1) Connexion
        mqttClient.connect(onConnected = {
            // 2) Tentative de purge du message retenu sur le topic Distance
            //    -> publier payload vide en retained pour effacer côté broker
            mqttClient.publish(topic = "Distance", payload = "", qos = 1, retained = true)

            // 3) Abonnement
            mqttClient.subscribe(topic = "Distance", qos = 1) { payload ->
                val newDist = parseDistanceSafe(payload) ?: return@subscribe

                // bornes simples (0 .. 1.2*max)
                if (newDist < 0f || newDist > maxDistance * 1.2f) return@subscribe

                // ignorer le tout premier message après abonnement (souvent retained)
                if (!firstMessageIgnored) {
                    firstMessageIgnored = true
                    return@subscribe
                }

                distance = newDist
                hasReading = true
                lastMsgTs = System.currentTimeMillis()
            }
        })

        onDispose {
            mqttClient.unsubscribe("Distance")
            mqttClient.disconnect()
            // reset visuel quand on quitte l'écran
            hasReading = false
            distance = maxDistance
        }
    }

    /* Retour auto à 0% après inactivité */
    LaunchedEffect(lastMsgTs, hasReading, maxDistance) {
        if (hasReading) {
            delay(inactivityMs)
            val now = System.currentTimeMillis()
            if (now - lastMsgTs >= inactivityMs) {
                hasReading = false
                distance = maxDistance // 0%
                firstMessageIgnored = false // prêt pour prochaine session
            }
        }
    }

    /* Notification si presque plein (uniquement si on a des vraies données) */
    LaunchedEffect(fillPercentage, hasReading) {
        if (hasReading && fillPercentage >= 95 && !notificationSent) {
            showFullNotification(context)
            notificationSent = true
        }
        if (fillPercentage < 90) {
            notificationSent = false
        }
    }

    /* UI */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        /* Logos top */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.isen),
                contentDescription = "ISEN Logo",
                modifier = Modifier.size(70.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.binovation_logo),
                contentDescription = "Binovation Logo",
                modifier = Modifier.size(70.dp)
            )
        }

        /* Carte status */
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.poubelle),
                    contentDescription = "Trash Bin",
                    modifier = Modifier.size(140.dp)
                )

                Text(
                    text = "Current fill level: ${fillPercentage.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                LinearProgressIndicator(
                    progress = fillPercentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = when {
                        fillPercentage >= 95 -> Color(0xFFC62828)
                        fillPercentage >= 80 -> Color(0xFFFF8F00)
                        fillPercentage >= 50 -> Color(0xFFFFD600)
                        else -> Color(0xFF43A047)
                    }
                )

                // Indication d'état pour compréhension
                val status = when {
                    !hasReading -> "Waiting for data..."
                    else -> "Live"
                }
                Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        /* Bouton Scan */
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // on réinitialise l'état avant une nouvelle session
                    hasReading = false
                    distance = maxDistance
                    firstMessageIgnored = false
                    mqttClient.publish("smartbin/scan", "start")
                    scanStatus = "Scan sent!"
                    delay(2500)
                    scanStatus = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Scan Now")
        }

        if (scanStatus.isNotEmpty()) {
            Text(
                text = scanStatus,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        /* Navigation */
        Text(
            "Navigation",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MenuCard("Bin Level") { navController.navigate("binLevel") }
                MenuCard("Bin State") { navController.navigate("binState") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MenuCard("History") { navController.navigate("history") }
                MenuCard("Analysis") { navController.navigate("analysis") }
            }
        }
    }
}

/* ====== Cartes menu (RowScope pour que weight() fonctionne) ====== */
@Composable
fun RowScope.MenuCard(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
