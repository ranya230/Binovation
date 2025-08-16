package fr.isen.amara.isensmartcompanion.screens

import org.json.JSONObject

/**
 * Parse différents formats MQTT pour la distance (mm).
 * Accepte :
 * - {"distance": 1200}
 * - {"Distance": 1200}
 * - 1200
 * - "Distance: 1200"
 */
fun parseDistance(message: String): Float? {
    // 1) JSON avec "distance" ou "Distance"
    try {
        val json = JSONObject(message)
        if (json.has("distance")) {
            val v = json.get("distance")
            return (v as? Number)?.toFloat() ?: (v as? String)?.toFloatOrNull()
        }
        if (json.has("Distance")) {
            val v = json.get("Distance")
            return (v as? Number)?.toFloat() ?: (v as? String)?.toFloatOrNull()
        }
    } catch (_: Exception) {
        // pas du JSON -> on continue
    }

    // 2) Nombre brut
    message.toFloatOrNull()?.let { return it }

    // 3) "Distance: 1234" ou similaire
    val regex = Regex("""-?\d+(\.\d+)?""")
    return regex.find(message)?.value?.toFloatOrNull()
}
