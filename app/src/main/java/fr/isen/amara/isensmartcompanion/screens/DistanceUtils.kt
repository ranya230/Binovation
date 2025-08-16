package fr.isen.amara.isensmartcompanion.screens

/**
 * Convertit une distance (mm) en pourcentage de remplissage.
 * - maxMm = hauteur interne max saisie par l’utilisateur (mm)
 * - Retourne un pourcentage borné [0..100]
 */
fun computeFillPercent(distanceMm: Float, maxMm: Float): Float {
    if (maxMm <= 0f) return 0f
    val pct = (maxMm - distanceMm) / maxMm * 100f
    return pct.coerceIn(0f, 100f)
}
