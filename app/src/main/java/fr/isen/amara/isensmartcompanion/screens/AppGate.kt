// AppGate.kt
package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import fr.isen.amara.isensmartcompanion.R

@Composable
fun AppGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppSettings.init(context) }
    val maxMm by AppSettings.maxDistanceMmFlow.collectAsState()

    // Tant que la hauteur n'est pas > 0, on reste bloqué sur l'écran de setup
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
    var unit by remember { mutableStateOf(UnitChoice.MM) } // mm par défaut

    val parsedMm = remember(valueText, unit) {
        val raw = valueText.trim().replace(',', '.')
        val n = raw.toFloatOrNull()
        val mm = when (unit) {
            UnitChoice.MM -> n
            UnitChoice.CM -> n?.times(10f)
            UnitChoice.M  -> n?.times(1000f)
        }
        mm?.takeIf { it > 0f } // doit être > 0
    }

    Box(Modifier
        .fillMaxSize()
        .padding(20.dp)) {

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logos (optionnel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.isen),
                    contentDescription = "ISEN logo",
                    modifier = Modifier.size(80.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.binovation_logo),
                    contentDescription = "Binovation logo",
                    modifier = Modifier.size(80.dp)
                )
            }

            Text("Set your bin height", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Enter the internal height of your bin. This is required to continue.")

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Value (${unit.label})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                UnitChoice.values().forEach { u ->
                    FilterChip(
                        selected = unit == u,
                        onClick = { unit = u },
                        label = { Text(u.label) }
                    )
                }
            }

            Button(
                onClick = { parsedMm?.let(onApply) },
                enabled = parsedMm != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply") }
        }
    }
}
