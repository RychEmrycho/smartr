package com.smartr.wear.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Smartr reminder")
            .setContentText(prompts.random(Random(System.currentTimeMillis())))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
