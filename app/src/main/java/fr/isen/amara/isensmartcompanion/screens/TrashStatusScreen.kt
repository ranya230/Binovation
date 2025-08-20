// TrashStatusScreen.kt
package fr.isen.amara.isensmartcompanion.screens

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
import androidx.navigation.NavController
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.RowScope

@Composable
fun TrashStatusScreen(navController: NavController) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppSettings.init(context) }

    val maxDistanceMm by AppSettings.maxDistanceMmFlow.collectAsState()
    val mqttClient = remember { MqttClientHelper(context) }
    val scope = rememberCoroutineScope()

    if (maxDistanceMm <= 0f) {
        Text("Please set your bin height first.")
        return
    }

    var hasReading by remember { mutableStateOf(false) }
    var distanceMm by remember { mutableStateOf(0f) }
    var lastMsgTs by remember { mutableStateOf(0L) }
    val inactivityMs = 12_000L

    val fillPercent = if (hasReading) {
        ((maxDistanceMm - distanceMm) / maxDistanceMm * 100f).coerceIn(0f, 100f)
    } else 0f

    DisposableEffect(maxDistanceMm) {
        if (!hasReading) distanceMm = 0f
        mqttClient.connectAndSubscribe("Distance") { payload ->
            val newDist = parseDistance(payload) ?: return@connectAndSubscribe
            distanceMm = newDist
            hasReading = true
            lastMsgTs = System.currentTimeMillis()
        }
        onDispose {
            mqttClient.unsubscribe("Distance")
            mqttClient.disconnect()
            hasReading = false
            distanceMm = 0f
        }
    }

    LaunchedEffect(lastMsgTs, hasReading, maxDistanceMm) {
        if (hasReading) {
            delay(inactivityMs)
            val now = System.currentTimeMillis()
            if (now - lastMsgTs >= inactivityMs) {
                hasReading = false
                distanceMm = 0f
            }
        }
    }

    var scanStatus by remember { mutableStateOf("") }
    var notificationSent by remember { mutableStateOf(false) }

    LaunchedEffect(fillPercent, hasReading) {
        if (hasReading && fillPercent >= 95 && !notificationSent) {
            showFullNotification(context)
            notificationSent = true
        }
        if (fillPercent < 90) notificationSent = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Header logos : même taille pour les deux ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerIconSize = 64.dp
            Image(painterResource(R.drawable.isen), contentDescription = "ISEN", modifier = Modifier.size(headerIconSize))
            Image(painterResource(R.drawable.binovation_logo), contentDescription = "Binovation", modifier = Modifier.size(headerIconSize))
        }

        // --- Carte niveau courant (inchangée) ---
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            var showEdit by remember { mutableStateOf(false) }
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bin height: ${maxDistanceMm.toInt()} mm", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showEdit = true }) { Text("Edit") }
            }
            if (showEdit) {
                MaxHeightDialog(
                    initialMm = maxDistanceMm,
                    onDismiss = { showEdit = false },
                    onApply = {
                        AppSettings.setMaxDistanceMm(context, it)
                        showEdit = false
                    }
                )
            }
        }

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
                    text = "Current fill level: ${fillPercent.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                LinearProgressIndicator(
                    progress = fillPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = when {
                        fillPercent >= 95 -> Color(0xFFC62828)
                        fillPercent >= 80 -> Color(0xFFFF8F00)
                        fillPercent >= 50 -> Color(0xFFFFD600)
                        else -> Color(0xFF43A047)
                    }
                )
                val status = if (!hasReading) "Waiting for data..." else "Live"
                Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    hasReading = false
                    distanceMm = 0f
                    mqttClient.publish("smartbin/scan", "start")
                    scanStatus = "Scan sent"
                    delay(2500)
                    scanStatus = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Scan Now") }

        if (scanStatus.isNotEmpty()) {
            Text(scanStatus, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }

        // --- NAV CARDS (texte “Navigation” supprimé, on garde la grille) ---
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

@Composable
private fun RowScope.MenuCard(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaxHeightDialog(
    initialMm: Float,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit
) {
    var unitIsMm by remember { mutableStateOf(true) }
    var numberText by remember { mutableStateOf(initialMm.toInt().toString()) }

    val parsedMm = remember(numberText, unitIsMm) {
        val n = numberText.trim().replace(',', '.').toFloatOrNull()
        val mm = if (n == null) null else if (unitIsMm) n else n * 10f
        mm?.takeIf { it > 0f }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit bin height") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = numberText,
                    onValueChange = { numberText = it },
                    label = { Text(if (unitIsMm) "Height (mm)" else "Height (cm)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = unitIsMm, onClick = {
                        if (!unitIsMm) {
                            val v = numberText.trim().replace(',', '.').toFloatOrNull()
                            numberText = v?.let { (it * 10f).toInt().toString() } ?: numberText
                            unitIsMm = true
                        }
                    }, label = { Text("mm") })
                    FilterChip(selected = !unitIsMm, onClick = {
                        if (unitIsMm) {
                            val v = numberText.trim().replace(',', '.').toFloatOrNull()
                            numberText = v?.let { (it / 10f).toString() } ?: numberText
                            unitIsMm = false
                        }
                    }, label = { Text("cm") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsedMm?.let(onApply) }, enabled = parsedMm != null) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
