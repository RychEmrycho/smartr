package com.smartr.wear.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

class ReminderScheduler(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "smartr_reminders"
    }

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Smartr Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    fun sendReminder() {
        val prompts = listOf(
            "Time to stand up and walk a bit.",
            "Hydrate: take a glass of water.",
            "Stretch break: loosen your shoulders."
        )
        val ackIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_ACKNOWLEDGE
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Smartr reminder")
            .setContentText(prompts.random(Random(System.currentTimeMillis())))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Done", ackPendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
