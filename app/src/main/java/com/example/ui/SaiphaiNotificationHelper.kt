package com.example.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import kotlin.random.Random

object SaiphaiNotificationHelper {
    private const val CHANNEL_ID = "saiphai_auto_updates"
    private const val CHANNEL_NAME = "Saiphai Auto Ride & Ticket Alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time booking, driver assignment, and fare notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendStatusNotification(context: Context, title: String, message: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(Random.nextInt(1000, 9999), notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }
}
