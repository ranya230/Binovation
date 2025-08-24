// AnalysisScreen.kt
package fr.isen.amara.isensmartcompanion.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalysisScreen() {
    val activity = LocalContext.current as ComponentActivity
    val vm: BinSharedViewModel = viewModel(
        activity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
    )

    val ui by vm.ui.collectAsState()
    val history = ui.history
    val lastUpdateStr = ui.lastUpdate?.let { formatDateTime(it) } ?: "—"

    val delta24h = remember(history.size) { deltaLastHours(24, history) }
    val slope10minPerMin = remember(history.size) { slopeLastMinutes(10, history) }
    val slope10minPerHour = slope10minPerMin?.let { it * 60f }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Analysis", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (ui.scanning) "Scanning..." else "Idle", fontSize = 12.sp, color = Color.Gray)
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Current Fill Level", fontWeight = FontWeight.Medium)
                LinearProgressIndicator(
                    progress = { (ui.percent ?: 0f) / 100f },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = levelColor(ui.percent ?: 0f),
                    trackColor = Color.LightGray
                )
                Text(ui.percent?.let { String.format("%.1f%%", it) } ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Last update: $lastUpdateStr", fontSize = 12.sp, color = Color.Gray)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

                val deltaTxt = delta24h?.let { String.format(Locale.getDefault(), "%+.1f %%", it) } ?: "—"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Δ last 24h")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (delta24h != null) {
                            Icon(if (delta24h >= 0) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, contentDescription = null, tint = if (delta24h >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                        Text(deltaTxt, fontWeight = FontWeight.Medium)
                    }
                }

                val rateTxt = slope10minPerHour?.let { String.format("%.2f %%/h", it) } ?: "—"
                KeyValueRow("Avg rate (10 min)", rateTxt)
            }
        }

        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                Text("All readings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                Divider()
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No data yet.") }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(history.asReversed()) { p -> ReadingRow(p) }
                    }
                }
            }
        }
    }
}

/* ===== UI bits ===== */

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReadingRow(p: HistoryPoint) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(String.format(Locale.getDefault(), "%.1f%%", p.percent), fontWeight = FontWeight.Medium)
        Text(formatTime(p.ts), fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun levelColor(percent: Float): Color =
    when {
        percent < 30f -> Color(0xFF2E7D32)
        percent < 70f -> Color(0xFFFFA000)
        else -> Color(0xFFC62828)
    }

/* ===== Helpers ===== */

private fun deltaLastHours(hours: Int, hist: List<HistoryPoint>): Float? {
    if (hist.size < 2) return null
    val now = System.currentTimeMillis()
    val from = now - hours * 3_600_000L
    val window = hist.filter { it.ts >= from }
    if (window.size < 2) return null
    return window.last().percent - window.first().percent
}

private fun slopeLastMinutes(minutes: Int, hist: List<HistoryPoint>): Float? {
    if (hist.size < 2) return null
    val now = System.currentTimeMillis()
    val fromTs = now - minutes * 60_000L
    val window = hist.filter { it.ts >= fromTs }
    if (window.size < 2) return null

    val t0 = window.first().ts
    var sumX = 0.0
    var sumY = 0.0
    var sumXX = 0.0
    var sumXY = 0.0
    val n = window.size.toDouble()

    window.forEach { p ->
        val x = (p.ts - t0).toDouble() / 60_000.0
        val y = p.percent.toDouble()
        sumX += x; sumY += y; sumXX += x * x; sumXY += x * y
    }
    val denom = n * sumXX - sumX * sumX
    if (denom == 0.0) return null
    return ((n * sumXY - sumX * sumY) / denom).toFloat()
}

private fun formatDateTime(millis: Long) =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

private fun formatTime(millis: Long) =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
