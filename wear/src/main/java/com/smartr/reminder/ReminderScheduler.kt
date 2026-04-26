package com.smartr.reminder

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
        private const val REMINDER_NOTIFICATION_ID = 1001
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

    fun sendReminder(sedentaryDurationSeconds: Int) {
        val durationFormatted = com.smartr.logic.DurationFormatter.format(context, sedentaryDurationSeconds)
        val titleText = context.getString(com.smartr.R.string.reminder_notification_title_format, durationFormatted)

        val prompts = context.resources.getStringArray(com.smartr.R.array.reminder_prompts)
        val prompt = prompts.random(Random(System.currentTimeMillis()))

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
            .setContentTitle(titleText)
            .setContentText(prompt)
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
        NotificationManagerCompat.from(context).notify(REMINDER_NOTIFICATION_ID, notification)
    }
}
