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

    @Volatile
    private var subscribed = false

    init {
        // 1) init settings + charge historique
        AppSettings.init(context)
        val (savedHistory, lastScan) = AnalysisStore.loadHistory(context)
        _ui.value = _ui.value.copy(history = savedHistory, lastUpdate = lastScan)

        // 2) se connecte et s’abonne UNE SEULE FOIS (activity-scoped VM)
        connectIfNeeded()
    }

    private fun connectIfNeeded() {
        if (subscribed) return
        _ui.value = _ui.value.copy(scanning = true)
        try {
            mqtt.connectAndSubscribe("Distance") { payload: String ->
                val dist = parseDistance(payload) ?: run {
                    Log.w("MQTT", "Cannot parse payload: $payload")
                    return@connectAndSubscribe
                }
                val max = AppSettings.maxDistanceMmFlow.value.takeIf { it > 0f } ?: run {
                    Log.w("MQTT", "Max distance not set yet, ignoring reading")
                    return@connectAndSubscribe
                }
                val percent = ((max - dist) / max * 100f).coerceIn(0f, 100f)

                viewModelScope.launch {
                    val now = System.currentTimeMillis()
                    val newPoint = HistoryPoint(percent = percent, ts = now)
                    val newHistory = _ui.value.history + newPoint

                    _ui.value = _ui.value.copy(
                        scanning = true,
                        percent = percent,
                        history = newHistory,
                        lastUpdate = now
                    )
                    AnalysisStore.saveHistory(context, newHistory)
                }
            }
            subscribed = true
        } catch (t: Throwable) {
            Log.e("MQTT", "Subscription failed", t)
            _ui.value = _ui.value.copy(scanning = false)
            subscribed = false
        }
    }

    /** Laisse l’API publique en place si tu l’utilises déjà. */
    fun start() = connectIfNeeded()

    /** NE PAS appeler depuis les écrans, on garde la connexion tant que l’activité vit. */
    fun stop() {
        try { mqtt.unsubscribe("Distance") } catch (t: Throwable) { Log.w("MQTT", "Unsubscribe error", t) }
        try { mqtt.disconnect() } catch (t: Throwable) { Log.w("MQTT", "Disconnect error", t) }
        subscribed = false
        _ui.value = _ui.value.copy(scanning = false)
    }

    fun triggerScan() {
        try { mqtt.publish("smartbin/scan", "start") }
        catch (t: Throwable) { Log.w("MQTT", "Publish scan failed", t) }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
