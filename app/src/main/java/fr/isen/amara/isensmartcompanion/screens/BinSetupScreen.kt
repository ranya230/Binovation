package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun BinSetupScreen(navController: NavController) {
    val context = LocalContext.current
    var valueText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(UnitChoice.MM) }

    // Conversion entrée → mm
    val parsedMm = remember(valueText, unit) {
        val raw = valueText.trim().replace(',', '.')
        val n = raw.toFloatOrNull()
        val mm = when (unit) {
            UnitChoice.MM -> n
            UnitChoice.CM -> n?.times(10f)
            UnitChoice.M -> n?.times(1000f)
        }
        mm?.takeIf { it > 0f }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set your bin height", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Enter the internal height of your bin before continuing.")

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = {
                    Text(
                        when (unit) {
                            UnitChoice.MM -> "Height (mm)"
                            UnitChoice.CM -> "Height (cm)"
                            UnitChoice.M -> "Height (m)"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Choix unité
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = unit == UnitChoice.MM,
                    onClick = { unit = UnitChoice.MM },
                    label = { Text("mm") }
                )
                FilterChip(
                    selected = unit == UnitChoice.CM,
                    onClick = { unit = UnitChoice.CM },
                    label = { Text("cm") }
                )
                FilterChip(
                    selected = unit == UnitChoice.M,
                    onClick = { unit = UnitChoice.M },
                    label = { Text("m") }
                )
            }

            // Bouton Next
            Button(
                onClick = {
                    parsedMm?.let {
                        AppSettings.setMaxDistanceMm(context, it)
                        // Aller à l'app principale
                        navController.navigate("main") {
                            popUpTo("binSetup") { inclusive = true }
                        }
                    }
                },
                enabled = parsedMm != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}
