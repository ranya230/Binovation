// TrashStatusScreen.kt
package fr.isen.amara.isensmartcompanion.screens

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.delay

@Composable
fun TrashStatusScreen(navController: NavController) {
    val context = LocalContext.current
    // init settings si besoin (idempotent)
    LaunchedEffect(Unit) { AppSettings.init(context) }
    val maxDistanceMm by AppSettings.maxDistanceMmFlow.collectAsState()

    // 🔗 ViewModel partagé à l’échelle de l’activité (UN SEUL pour tout le nav)
    val activity = LocalContext.current as ComponentActivity
    val vm: BinSharedViewModel = viewModel(
        activity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
    )

    val ui by vm.ui.collectAsState()

    if (maxDistanceMm <= 0f) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Please set your bin height first.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Button(onClick = { navController.navigate("binSetup") }) { Text("Set bin height") }
            }
        }
        return
    }

    val fillPercent = (ui.percent ?: 0f).coerceIn(0f, 100f)
    val inactivityMs = 12_000L
    val live = ui.lastUpdate?.let { System.currentTimeMillis() - it < inactivityMs } ?: false

    var notificationSent by remember { mutableStateOf(false) }
    LaunchedEffect(fillPercent, live) {
        if (live && fillPercent >= 95f && !notificationSent) {
            showFullNotification(context)
            notificationSent = true
        }
        if (fillPercent < 90f) notificationSent = false
    }

    var scanStatus by remember { mutableStateOf("") }
    LaunchedEffect(scanStatus) {
        if (scanStatus.isNotEmpty()) {
            delay(2500)
            scanStatus = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val headerIconSize = 64.dp
            Image(painterResource(R.drawable.isen), contentDescription = "ISEN", modifier = Modifier.size(headerIconSize))
            Image(painterResource(R.drawable.binovation_logo), contentDescription = "Binovation", modifier = Modifier.size(headerIconSize))
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            var showEdit by remember { mutableStateOf(false) }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(id = R.drawable.poubelle), contentDescription = "Trash Bin", modifier = Modifier.size(140.dp))
                Text("Current fill level: ${fillPercent.toInt()}%", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                LinearProgressIndicator(
                    progress = fillPercent / 100f,
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = when {
                        fillPercent >= 95f -> Color(0xFFC62828)
                        fillPercent >= 80f -> Color(0xFFFF8F00)
                        fillPercent >= 50f -> Color(0xFFFFD600)
                        else -> Color(0xFF43A047)
                    }
                )
                Text(if (live) "Live" else "Waiting for data...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = {
                vm.triggerScan()
                scanStatus = "Scan sent"
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Scan Now") }

        if (scanStatus.isNotEmpty()) {
            Text(scanStatus, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
        modifier = Modifier.weight(1f).height(88.dp),
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
    var unit by remember { mutableStateOf(UnitChoice.MM) }
    var numberText by remember { mutableStateOf("") }

    val parsedMm = remember(numberText, unit) {
        val n = numberText.trim().replace(',', '.').toFloatOrNull()
        val mm = when (unit) {
            UnitChoice.MM -> n
            UnitChoice.CM -> n?.times(10f)
            UnitChoice.M -> n?.times(1000f)
        }
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
                    label = { Text(when (unit) { UnitChoice.MM -> "Height (mm)"; UnitChoice.CM -> "Height (cm)"; UnitChoice.M -> "Height (m)" }) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = unit == UnitChoice.MM, onClick = { unit = UnitChoice.MM }, label = { Text("mm") })
                    FilterChip(selected = unit == UnitChoice.CM, onClick = { unit = UnitChoice.CM }, label = { Text("cm") })
                    FilterChip(selected = unit == UnitChoice.M, onClick = { unit = UnitChoice.M }, label = { Text("m") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { parsedMm?.let(onApply) }, enabled = parsedMm != null) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
