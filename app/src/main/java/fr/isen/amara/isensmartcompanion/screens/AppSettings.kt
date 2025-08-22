// AppSettings.kt
package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppSettings {
    private const val PREFS = "app_prefs"
    private const val KEY_MAX_MM = "max_distance_mm"

    // Sentinelle = -1f => pas encore configuré par l'utilisateur
    private val _maxDistanceMm = MutableStateFlow(-1f)
    val maxDistanceMmFlow: StateFlow<Float> = _maxDistanceMm

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _maxDistanceMm.value = prefs.getFloat(KEY_MAX_MM, -1f)
    }

    fun setMaxDistanceMm(context: Context, mm: Float) {
        val v = if (mm > 0f) mm else -1f
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_MAX_MM, v).apply()
        _maxDistanceMm.value = v
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MAX_MM).apply()
        _maxDistanceMm.value = -1f
    }
}
