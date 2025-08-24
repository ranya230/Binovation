package fr.isen.amara.isensmartcompanion.screens

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.isen.amara.isensmartcompanion.R

/**
 * Affiche une notification système lorsque la poubelle est presque pleine.
 * Cette fonction suppose que la permission POST_NOTIFICATIONS a déjà été accordée.
 */
@SuppressLint("MissingPermission")
fun showFullNotification(context: Context) {
    val channelId = "bin_full_alert_channel"
    val notificationId = 1001

    // Création du canal de notification (obligatoire pour Android 8+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Bin Full Alert"
        val descriptionText = "Notifications when bin is almost full"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Construction de la notification
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification) // Assure-toi d’avoir une icône valide
        .setContentTitle("Bin status alert")
        .setContentText("The bin is 95% full or more.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    // Affichage
    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
}
