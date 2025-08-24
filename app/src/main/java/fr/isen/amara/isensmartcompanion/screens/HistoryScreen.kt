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

    // ===== Filtres (état regroupé) =====
    var filters by remember {
        mutableStateOf(FilterState(window = TimeWindow.ALL, level = LevelFilter.ALL, date = null))
    }

    val availableDates = remember(fullHistory) {
        fullHistory.map { formatDateOnly(it.ts) }.distinct().sortedDescending()
    }

    // ===== Application des filtres =====
    val now = System.currentTimeMillis()
    val filtered = remember(fullHistory, filters, now) {
        applyFilters(
            history = fullHistory,
            window = filters.window,
            level = filters.level,
            selectedDate = filters.date,
            now = now
        )
    }

    // ===== Stats rapides =====
    val minVal = filtered.minOfOrNull { it.percent }?.roundToInt()
    val maxVal = filtered.maxOfOrNull { it.percent }?.roundToInt()
    val avgVal = filtered.takeIf { it.isNotEmpty() }
        ?.map { it.percent }?.average()
        ?.let { String.format(Locale.getDefault(), "%.1f", it) }

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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(if (ui.scanning) "Listening..." else "Idle", fontSize = 12.sp, color = Color.Gray)
        }

        // Filtres
        FilterBar(
            window = filters.window,
            onWindowChange = { w -> filters = filters.copy(window = w, date = null) },
            level = filters.level,
            onLevelChange = { lf -> filters = filters.copy(level = lf) },
            date = filters.date,
            onDateChange = { d -> filters = filters.copy(date = d) },
            availableDates = availableDates
        )

        // Stats
        StatsCard(minVal = minVal, maxVal = maxVal, avgVal = avgVal, count = filtered.size)

        // Liste
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

/* ==================== Filtrage ==================== */

private data class FilterState(
    val window: TimeWindow,
    val level: LevelFilter,
    val date: String?
)

private fun applyFilters(
    history: List<HistoryPoint>,
    window: TimeWindow,
    level: LevelFilter,
    selectedDate: String?,
    now: Long
): List<HistoryPoint> {
    val fromTs = timeFrom(window, now)

    val base = history
        .asSequence()
        .filter { it.ts >= fromTs }
        .filter { hp -> selectedDate?.let { formatDateOnly(hp.ts) == it } ?: true }
        .toList()

    return base.filter { hp ->
        when (level) {
            LevelFilter.ALL -> true
            LevelFilter.LOW -> hp.percent < 50f
            LevelFilter.MEDIUM -> hp.percent in 50f..79.999f
            LevelFilter.HIGH -> hp.percent in 80f..94.999f
            LevelFilter.FULL -> hp.percent >= 95f
            LevelFilter.EMPTY -> hp.percent == 0f
        }
    }
}

private fun timeFrom(window: TimeWindow, now: Long): Long =
    when (window) {
        TimeWindow.H1 -> now - 1L * 60 * 60 * 1000
        TimeWindow.H6 -> now - 6L * 60 * 60 * 1000
        TimeWindow.H24 -> now - 24L * 60 * 60 * 1000
        TimeWindow.ALL -> Long.MIN_VALUE
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Fenêtre temporelle
        Text("Time window", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ChipRowEqual {
            TimeWindow.values().forEach { w ->
                EqualFilterChip(
                    selected = window == w,
                    onClick = { onWindowChange(w) },
                    label = w.label
                )
            }
        }

        // Niveaux
        Text("Level", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ChipRowEqual {
            LevelFilter.values().forEach { lf ->
                LevelFilterChip(
                    level = lf,
                    selected = level == lf,
                    onClick = { onLevelChange(lf) }
                )
            }
        }

        // Dates
        Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        val dateLabels = listOf<String?>(null) + availableDates
        dateLabels.chunked(4).forEach { row ->
            ChipRowEqual(expectedItems = 4, actualItems = row.size) {
                row.forEach { d ->
                    EqualFilterChip(
                        selected = date == d,
                        onClick = { onDateChange(d) },
                        label = d ?: "All dates"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRowEqual(
    expectedItems: Int? = null,
    actualItems: Int? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
        // Pour compléter la ligne si moins de 4 éléments
        if (expectedItems != null && actualItems != null && actualItems < expectedItems) {
            repeat(expectedItems - actualItems) {
                Spacer(
                    modifier = Modifier.width(90.dp)
                )
            }
        }
    }
}

@Composable
private fun EqualFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = Modifier
            .width(90.dp)   // largeur fixe pour alignement — ajuste selon le längueur de tes labels
            .height(40.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun LevelFilterChip(
    level: LevelFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (selContainer, selLabel) = when (level) {
        LevelFilter.ALL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) to MaterialTheme.colorScheme.onSurface
        LevelFilter.LOW -> Color(0xFF43A047).copy(alpha = 0.18f) to Color(0xFF1B5E20)
        LevelFilter.MEDIUM -> Color(0xFFFFD600).copy(alpha = 0.24f) to Color(0xFF8D6E00)
        LevelFilter.HIGH -> Color(0xFFFF8F00).copy(alpha = 0.18f) to Color(0xFFE65100)
        LevelFilter.FULL -> Color(0xFFC62828).copy(alpha = 0.18f) to Color(0xFFB71C1C)
        LevelFilter.EMPTY -> Color(0xFFC8E6C9).copy(alpha = 0.50f) to Color(0xFF2E7D32)
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(level.label, maxLines = 1) },
        modifier = Modifier
            .width(90.dp)
            .height(40.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selContainer,
            selectedLabelColor = selLabel
        )
    )
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
        p == 0f -> Color(0xFFC8E6C9) to Color(0xFF2E7D32)
        p >= 95f -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        p >= 80f -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        p >= 50f -> Color(0xFFFFFDE7) to Color(0xFFFBC02D)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
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
    EMPTY("Empty")
}

private fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatHourMinute(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
