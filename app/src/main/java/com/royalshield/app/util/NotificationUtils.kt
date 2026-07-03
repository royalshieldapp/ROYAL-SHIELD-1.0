package com.royalshield.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.royalshield.app.MainActivity
import com.royalshield.app.R

object NotificationUtils {

    const val CHANNEL_SECURITY_ALERTS = "security_alerts"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for real-time security threats"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_SECURITY_ALERTS, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                lightColor = android.graphics.Color.RED
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSecurityAlert(context: Context, title: String, message: String, isCritical: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SECURITY_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback to system icon for stability
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(if (isCritical) 0xFFFF1744.toInt() else 0xFFFFC107.toInt()) // NeonRed vs RoyalGold
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
