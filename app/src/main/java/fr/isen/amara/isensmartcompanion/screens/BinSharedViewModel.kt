// BinSharedViewModel.kt
package fr.isen.amara.isensmartcompanion.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val scanning: Boolean = false,
    val percent: Float? = null,
    val history: List<HistoryPoint> = emptyList(),
    val lastUpdate: Long? = null
)

class BinSharedViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val mqtt by lazy { MqttClientHelper(app.applicationContext) }
    private val context = app.applicationContext

    init {
        AppSettings.init(context) // charge la valeur max si déjà enregistrée
        val (savedHistory, lastScan) = AnalysisStore.loadHistory(context)
        _ui.value = _ui.value.copy(history = savedHistory, lastUpdate = lastScan)
    }

    fun start() {
        _ui.value = _ui.value.copy(scanning = true)

        mqtt.connectAndSubscribe("Distance") { payload: String ->
            val dist = parseDistance(payload)
            if (dist == null) {
                Log.w("MQTT", "Cannot parse payload: $payload")
                return@connectAndSubscribe
            }

            val max = AppSettings.maxDistanceMmFlow.value.takeIf { it > 0f }
            if (max == null) {
                Log.w("MQTT", "Max distance not set yet, ignoring reading")
                return@connectAndSubscribe
            }

            val percent = ((max - dist) / max * 100f).coerceIn(0f, 100f)

            viewModelScope.launch {
                val newHistory = _ui.value.history + HistoryPoint(
                    percent = percent,
                    ts = System.currentTimeMillis()
                )
                _ui.value = _ui.value.copy(
                    scanning = true,
                    percent = percent,
                    history = newHistory,
                    lastUpdate = System.currentTimeMillis()
                )
                AnalysisStore.saveHistory(context, newHistory)
            }
        }
    }

    fun stop() {
        mqtt.unsubscribe("Distance")
        mqtt.disconnect()
        _ui.value = _ui.value.copy(scanning = false)
    }
}
