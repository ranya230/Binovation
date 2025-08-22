// BinStateScreen.kt
package fr.isen.amara.isensmartcompanion.screens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@Composable
fun BinStateScreen() {
    val app = LocalContext.current.applicationContext as Application
    val vm: BinSharedViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    LaunchedEffect(Unit) { vm.start() }

    val ui by vm.ui.collectAsState()
    val percent = ui.percent ?: 0f
    val history = ui.history

    // Couleur selon niveau
    val levelColor = when {
        percent >= 95f -> Color(0xFFC62828)
        percent >= 80f -> Color(0xFFFF8F00)
        percent >= 50f -> Color(0xFFFFD600)
        else -> Color(0xFF43A047)
    }

    // Statut texte
    val statusText = when {
        percent >= 95f -> "Full"
        percent >= 80f -> "High"
        percent >= 50f -> "Medium"
        percent >= 10f -> "Low"
        else -> "Empty"
    }

    // Trend basé sur la pente des 10 dernières minutes
    val slopePerMin = remember(history.size) { slopeLastMinutes(10, history) ?: 0f }
    val trend = when {
        slopePerMin > 0.05f -> "Increasing"
        slopePerMin < -0.05f -> "Decreasing"
        else -> "Stable"
    }

    // Dernière mise à jour
    val lastUpdateTxt = ui.lastUpdate?.let { secondsAgoString(it) } ?: "—"
    val outdated = ui.lastUpdate?.let { System.currentTimeMillis() - it > 10 * 60_000 } ?: false

    // Moyenne des 5 dernières mesures
    val avg5 = if (history.size >= 5) history.takeLast(5).map { it.percent }.average() else null

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Bin State", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        // Carte statut actuel
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current level", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = levelColor,
                    trackColor = Color.LightGray
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        String.format(Locale.getDefault(), "%.1f%% (%s)", percent, statusText),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Last update: $lastUpdateTxt",
                        fontSize = 12.sp,
                        color = if (outdated) Color.Red else Color.Gray
                    )
                }
                avg5?.let {
                    Text(
                        "Avg (last 5): %.1f%%".format(it),
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // Avertissement seuil critique
        if (percent >= 80f) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚠ Bin almost full (≥80%)",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Carte tendance + sparkline
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Trend", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status: $trend")
                    val ratePerHour = slopePerMin * 60f
                    Text(String.format(Locale.getDefault(), "Rate: %.2f %%/h", ratePerHour))
                }

                Sparkline(
                    points = history.map { it.percent },
                    strokeColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
        }

        // Carte lectures complètes
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    "All readings",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data yet.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(history.asReversed()) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    String.format(Locale.getDefault(), "%.1f%%", p.percent),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(formatTime(p.ts), fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ===== Helpers ===== */

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

private fun secondsAgoString(ts: Long): String {
    val s = ((System.currentTimeMillis() - ts) / 1000).toInt()
    return when {
        s < 60 -> "$s s ago"
        s < 3600 -> "${s / 60} min ago"
        else -> "${s / 3600} h ago"
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

/* ===== Sparkline corrigé (moveTo/lineTo avec x,y Float) ===== */
@Composable
private fun Sparkline(
    points: List<Float>,
    strokeColor: Color,
    modifier: Modifier = Modifier,
    padding: Float = 8f
) {
    if (points.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data", fontSize = 12.sp, color = Color.Gray)
        }
        return
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minY = points.minOrNull() ?: 0f
        val maxY = points.maxOrNull() ?: 100f
        val spanY = kotlin.math.max(1f, maxY - minY)

        val stepX = (w - 2 * padding) / (points.size - 1).coerceAtLeast(1)
        val path = Path()

        points.forEachIndexed { i, v ->
            val x = padding + i * stepX
            val y = padding + (h - 2 * padding) * (1f - (v - minY) / spanY)
            if (i == 0) {
                path.moveTo(x, y)      // ✅ utilise (x, y)
            } else {
                path.lineTo(x, y)      // ✅ utilise (x, y)
            }
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
