package com.smartr.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
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
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    fun sendReminder(sedentaryDurationSeconds: Int, thresholdSeconds: Int) {
        val notificationId = Random.nextInt(1000, 9999)
        val durationFormatted = com.smartr.logic.DurationFormatter.format(context, sedentaryDurationSeconds)
        val criticality = com.smartr.logic.SedentaryCriticality.fromDuration(sedentaryDurationSeconds, thresholdSeconds)
        val emoji = criticality.emoji
        val titleText = "$emoji " + context.getString(com.smartr.R.string.reminder_notification_title_format, durationFormatted)

        val prompts = context.resources.getStringArray(com.smartr.R.array.reminder_prompts)
        val prompt = prompts.random(Random(System.currentTimeMillis()))

        val ackIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_ACKNOWLEDGE
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, com.smartr.MainActivity::class.java)
        val options = ActivityOptions.makeBasic()
        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options.toBundle()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(prompt)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Done", ackPendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
