// Utils.kt (ou où tu veux)
package fr.isen.amara.isensmartcompanion.screens

import org.json.JSONObject

/**
 * Retourne la distance en millimètres si possible.
 * Accepte :
 *  - JSON: {"distance":123} ou {"distance":123.4}
 *  - Texte brut: "123", "Distance: 123 mm", "distance=12.3 cm", "0.45 m", etc.
 */
fun parseDistance(payload: String): Float? {
    val s = payload.trim()

    // 1) Essayer JSON
    try {
        val obj = JSONObject(s)
        if (obj.has("distance")) {
            return obj.getDouble("distance").toFloat() // on suppose déjà en mm côté JSON
        }
    } catch (_: Exception) {
        // pas du JSON → on tente texte
    }

    // 2) Texte brut : on capture le 1er nombre (int/float) + éventuelle unité
    val regex = Regex("""(-?\d+(?:[.,]\d+)?)\s*(mm|cm|m)?""", RegexOption.IGNORE_CASE)
    val m = regex.find(s) ?: return null

    val valueStr = m.groupValues[1].replace(',', '.')
    val value = valueStr.toFloatOrNull() ?: return null
    val unit = m.groupValues.getOrNull(2)?.lowercase()

    // conversion → mm
    val mm = when (unit) {
        "cm" -> value * 10f
        "m"  -> value * 1000f
        else -> value // mm par défaut ou unité absente
    }

    return mm
}
