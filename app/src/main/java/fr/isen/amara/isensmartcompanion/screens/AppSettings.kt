// AppSettings.kt
package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppSettings {
    private const val PREFS = "app_prefs"
    private const val KEY_MAX_MM = "max_distance_mm"

    private val _maxDistanceMm = MutableStateFlow(0f)
    val maxDistanceMmFlow: StateFlow<Float> = _maxDistanceMm

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _maxDistanceMm.value = prefs.getFloat(KEY_MAX_MM, 0f)
    }

    fun setMaxDistanceMm(context: Context, mm: Float) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_MAX_MM, mm).apply()
        _maxDistanceMm.value = mm
    }
}
