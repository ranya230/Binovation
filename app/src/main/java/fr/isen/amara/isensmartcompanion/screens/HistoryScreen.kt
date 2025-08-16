package fr.isen.amara.isensmartcompanion.screens

import android.app.Application
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

@Composable
fun HistoryScreen() {
    val app = LocalContext.current.applicationContext as Application
    val vm: BinSharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    LaunchedEffect(Unit) { vm.start() }

    val ui by vm.ui.collectAsState()
    val fullHistory = ui.history

    // ===== Filtres =====
    var window by remember { mutableStateOf(TimeWindow.ALL) }
    var level by remember { mutableStateOf(LevelFilter.ALL) }
    var selectedDate by remember { mutableStateOf<String?>(null) } // si != null, on affiche uniquement cette date

    // Liste des dates disponibles (yyyy-MM-dd)
    val availableDates = remember(fullHistory) {
        fullHistory.map { formatDateOnly(it.ts) }.distinct().sortedDescending()
    }

    // ===== Application des filtres =====
    val now = System.currentTimeMillis()
    val fromTs = when (window) {
        TimeWindow.H1  -> now - 1L * 60 * 60 * 1000
        TimeWindow.H6  -> now - 6L * 60 * 60 * 1000
        TimeWindow.H24 -> now - 24L * 60 * 60 * 1000
        TimeWindow.ALL -> Long.MIN_VALUE
    }

    val baseFiltered = remember(fullHistory, window, selectedDate) {
        fullHistory
            .asSequence()
            .filter { it.ts >= fromTs }
            .filter { sel ->
                selectedDate?.let { formatDateOnly(sel.ts) == it } ?: true
            }
            .toList()
    }

    val filtered = remember(baseFiltered, level) {
        baseFiltered.filter { hp ->
            when (level) {
                LevelFilter.ALL     -> true
                LevelFilter.LOW     -> hp.percent < 50f
                LevelFilter.MEDIUM  -> hp.percent in 50f..79.999f
                LevelFilter.HIGH    -> hp.percent in 80f..94.999f
                LevelFilter.FULL    -> hp.percent >= 95f
                LevelFilter.EMPTIES -> hp.percent == 0f
            }
        }
    }

    // ===== Stats rapides sur l’échantillon filtré =====
    val minVal = filtered.minOfOrNull { it.percent }?.roundToInt()
    val maxVal = filtered.maxOfOrNull { it.percent }?.roundToInt()
    val avgVal = filtered.takeIf { it.isNotEmpty() }?.map { it.percent }?.average()?.let { String.format(Locale.getDefault(), "%.1f", it) }

    // ===== Groupement par date =====
    val grouped = remember(filtered) {
        filtered.sortedByDescending { it.ts }.groupBy { formatDateOnly(it.ts) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(if (ui.scanning) "Listening..." else "Idle", fontSize = 12.sp, color = Color.Gray)
        }

        // Filtres
        FilterBar(
            window = window,
            onWindowChange = { window = it; selectedDate = null }, // si on change de fenêtre, on libère la date spécifique
            level = level,
            onLevelChange = { level = it },
            date = selectedDate,
            onDateChange = { selectedDate = it },
            availableDates = availableDates
        )

        // Stats rapides
        StatsCard(minVal = minVal, maxVal = maxVal, avgVal = avgVal, count = filtered.size)

        // Liste groupée
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching data.", fontSize = 16.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (date, items) ->
                    item {
                        Text(
                            text = date,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                        Divider()
                    }
                    items(items, key = { it.ts }) { hp ->
                        HistoryRow(hp)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/* ==================== UI Blocks ==================== */

@Composable
private fun FilterBar(
    window: TimeWindow,
    onWindowChange: (TimeWindow) -> Unit,
    level: LevelFilter,
    onLevelChange: (LevelFilter) -> Unit,
    date: String?,
    onDateChange: (String?) -> Unit,
    availableDates: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Fenêtre temporelle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeWindow.values().forEach { w ->
                FilterChip(
                    selected = window == w,
                    onClick = { onWindowChange(w) },
                    label = { Text(w.label) }
                )
            }
        }

        // Niveau
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelFilter.values().forEach { lf ->
                FilterChip(
                    selected = level == lf,
                    onClick = { onLevelChange(lf) },
                    label = { Text(lf.label) }
                )
            }
        }

        // Date précise (optionnelle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { onDateChange(null) },
                label = { Text("All dates") },
                leadingIcon = {},
                enabled = date != null
            )
            availableDates.take(7).forEach { d -> // on affiche les 7 dernières dates pour rester compact
                FilterChip(
                    selected = date == d,
                    onClick = { onDateChange(d) },
                    label = { Text(d) }
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    minVal: Int?,
    maxVal: Int?,
    avgVal: String?,
    count: Int
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Summary", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Count"); Text("$count", fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Min"); Text(minVal?.let { "$it%" } ?: "—", fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max"); Text(maxVal?.let { "$it%" } ?: "—", fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Average"); Text(avgVal?.let { "$it%" } ?: "—", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HistoryRow(hp: HistoryPoint) {
    val p = hp.percent
    val (bg, txt) = when {
        p >= 95f -> Color(0xFFFFEBEE) to Color(0xFFC62828) // full
        p >= 80f -> Color(0xFFFFF3E0) to Color(0xFFEF6C00) // high
        p >= 50f -> Color(0xFFFFFDE7) to Color(0xFFFBC02D) // medium
        else     -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(String.format(Locale.getDefault(), "%.1f%%", p), fontWeight = FontWeight.Medium, color = txt)
            Text(formatHourMinute(hp.ts), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

/* ==================== Types & Helpers ==================== */

private enum class TimeWindow(val label: String) {
    H1("1h"), H6("6h"), H24("24h"), ALL("All")
}

private enum class LevelFilter(val label: String) {
    ALL("All"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    FULL("Full"),
    EMPTIES("Empties")
}

private fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatHourMinute(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
