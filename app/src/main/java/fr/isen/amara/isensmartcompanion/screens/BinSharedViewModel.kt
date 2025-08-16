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
        // Charger historique au démarrage
        val (savedHistory, lastScan) = AnalysisStore.loadHistory(context)
        _ui.value = _ui.value.copy(
            history = savedHistory,
            lastUpdate = lastScan
        )
    }

    fun start() {
        _ui.value = _ui.value.copy(scanning = true)

        mqtt.connectAndSubscribe("Distance") { payload: String ->
            Log.d("MQTT", "Payload reçu: $payload")

            val dist = parseDistance(payload)
            Log.d("MQTT", "Distance parsée: $dist")

            dist?.let {
                val maxDistance = 1200f // valeur forcée pour le test
                Log.d("MQTT", "Max distance forcée: $maxDistance mm")

                val percent = ((maxDistance - it) / maxDistance * 100f)
                    .coerceIn(0f, 100f)
                Log.d("MQTT", "Pourcentage calculé: $percent %")

                viewModelScope.launch {
                    val newHistory = _ui.value.history + HistoryPoint(
                        percent,
                        System.currentTimeMillis()
                    )

                    _ui.value = _ui.value.copy(
                        scanning = true,
                        percent = percent,
                        history = newHistory,
                        lastUpdate = System.currentTimeMillis()
                    )

                    // Sauvegarde
                    AnalysisStore.saveHistory(context, newHistory)
                }
            } ?: Log.w("MQTT", "Impossible de parser le payload")
        }
    }

    fun stop() {
        mqtt.unsubscribe("Distance")
        mqtt.disconnect()
        _ui.value = _ui.value.copy(scanning = false)
    }
}
