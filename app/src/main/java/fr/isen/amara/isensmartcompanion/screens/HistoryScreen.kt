package fr.isen.amara.isensmartcompanion.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class HistoryEntry(val percentage: Float, val timestamp: Long)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val mqtt = remember { MqttClientHelper(context) }

    // ⚠️ mets une valeur réaliste pour tes tests Python (ex: 1200 mm)
    val maxDistance = getMaxDistance(context).takeIf { it > 0 } ?: 1200f

    val history = remember { mutableStateListOf<HistoryEntry>() }
    var lastRaw by remember { mutableStateOf<String?>(null) }   // DEBUG: dernier payload
    var count by remember { mutableStateOf(0) }                  // DEBUG: nb messages reçus

    var selectedDate by remember { mutableStateOf("All") }
    var selectedFilter by remember { mutableStateOf("All") }

    val availableDates = remember(history) {
        history.map { formatDateOnly(it.timestamp) }.distinct().sortedDescending()
    }

    DisposableEffect(Unit) {
        mqtt.connectAndSubscribe("Distance") { message ->
            // DEBUG
            Log.d("HISTORY", "MQTT payload: $message")
            lastRaw = message
            count++

            val distance = parseDistance(message) ?: return@connectAndSubscribe
            val percentage = ((maxDistance - distance) / maxDistance * 100f).coerceIn(0f, 100f)
            val timestamp = System.currentTimeMillis()

            history.add(HistoryEntry(percentage, timestamp))
            if (history.size > 200) history.removeAt(0)
        }
        onDispose {
            mqtt.unsubscribe("Distance")
            mqtt.disconnect()
        }
    }

    val filteredHistory = remember(history, selectedDate, selectedFilter) {
        history.filter {
            val matchDate = selectedDate == "All" || formatDateOnly(it.timestamp) == selectedDate
            val matchFilter = when (selectedFilter) {
                "Full"  -> it.percentage >= 95f
                "Empty" -> it.percentage == 0f
                "Low"   -> it.percentage < 50f
                else    -> true
            }
            matchDate && matchFilter
        }.sortedByDescending { it.timestamp }
            .groupBy { formatDateOnly(it.timestamp) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // DEBUG (visible écran): pour vérifier que ça écoute
        Text("Messages reçus: $count", fontSize = 12.sp, color = Color.Gray)
        lastRaw?.let { Text("Dernier payload: $it", fontSize = 12.sp, color = Color.Gray) }

        Spacer(Modifier.height(12.dp))

        FilterRow(
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it },
            dateOptions = listOf("All") + availableDates,
            selectedType = selectedFilter,
            onTypeChange = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching data.", fontSize = 16.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredHistory.forEach { (date, entries) ->
                    item {
                        Text(
                            text = "📅 $date",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(entries, key = { it.timestamp }) { entry ->
                        HistoryCard(entry)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterRow(
    selectedDate: String,
    onDateChange: (String) -> Unit,
    dateOptions: List<String>,
    selectedType: String,
    onTypeChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        DropdownFilter(
            label = "Date",
            selected = selectedDate,
            options = dateOptions,
            onSelect = onDateChange
        )
        DropdownFilter(
            label = "Filter",
            selected = selectedType,
            options = listOf("All", "Full", "Empty", "Low"),
            onSelect = onTypeChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(entry: HistoryEntry) {
    val fill = entry.percentage.roundToInt()
    val time = formatHourMinute(entry.timestamp)

    val (bgColor, label, textColor) = when {
        fill >= 95 -> Triple(Color(0xFFFFEBEE), "Bin full: $fill%", Color(0xFFC62828))
        fill == 0 -> Triple(Color(0xFFC8E6C9), "Bin emptied: 0%", Color(0xFF2E7D32))
        else -> Triple(Color(0xFFF1F0FC), "Update: $fill%", MaterialTheme.colorScheme.onSurface)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 16.sp, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Time: $time", fontSize = 13.sp)
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    return sdf.format(Date(millis))
}

@SuppressLint("SimpleDateFormat")
fun formatHourMinute(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm")
    return sdf.format(Date(millis))
}
