// AppGate.kt
package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier

@Composable
fun AppGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppSettings.init(context) }
    val maxMm by AppSettings.maxDistanceMmFlow.collectAsState()

    if (maxMm <= 0f) {
        BinHeightSetup(
            onApply = { valueMm -> AppSettings.setMaxDistanceMm(context, valueMm) }
        )
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BinHeightSetup(onApply: (Float) -> Unit) {
    var valueText by remember { mutableStateOf("") }
    var unitIsMm by remember { mutableStateOf(true) } // true = mm, false = cm

    val parsedMm = remember(valueText, unitIsMm) {
        val raw = valueText.trim().replace(',', '.')
        val n = raw.toFloatOrNull()
        val mm = if (n == null) null else if (unitIsMm) n else n * 10f
        // pas de min/max imposés ; juste > 0 pour éviter division par zéro
        mm?.takeIf { it > 0f }
    }

    Box(Modifier.fillMaxSize().padding(20.dp)) {
        Column(
            Modifier.fillMaxWidth().align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Set your bin height", style = MaterialTheme.typography.headlineSmall)
            Text("Enter the internal height of your bin. All screens will use it.")

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text(if (unitIsMm) "Value (mm)" else "Value (cm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = unitIsMm,
                    onClick = { unitIsMm = true },
                    label = { Text("mm") }
                )
                FilterChip(
                    selected = !unitIsMm,
                    onClick = { unitIsMm = false },
                    label = { Text("cm") }
                )
            }

            Button(
                onClick = { parsedMm?.let(onApply) },
                enabled = parsedMm != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply") }
        }
    }
}
