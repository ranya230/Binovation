package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persiste l'historique d'analyse jusqu'au prochain scan.
 * Stockage simple via SharedPreferences en JSON.
 */
object AnalysisStore {
    private const val PREF = "analysis_store"
    private const val KEY_HISTORY = "history" // JSONArray d'objets { percent, ts }
    private const val KEY_LAST_SCAN_TS = "last_scan_ts" // Long

    fun saveHistory(context: Context, history: List<HistoryPoint>) {
        val arr = JSONArray()
        history.forEach { p ->
            arr.put(JSONObject().apply {
                put("percent", p.percent)
                put("ts", p.ts)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, arr.toString())
            .putLong(KEY_LAST_SCAN_TS, history.lastOrNull()?.ts ?: 0L)
            .apply()
    }

    fun loadHistory(context: Context): Pair<List<HistoryPoint>, Long?> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList<HistoryPoint>() to null
        val arr = JSONArray(raw)
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    HistoryPoint(
                        percent = o.optDouble("percent", 0.0).toFloat(),
                        ts = o.optLong("ts", 0L)
                    )
                )
            }
        }
        val lastScanTs = prefs.getLong(KEY_LAST_SCAN_TS, 0L).takeIf { it > 0L }
        return list to lastScanTs
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .remove(KEY_LAST_SCAN_TS)
            .apply()
    }
}
