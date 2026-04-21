package com.smartr.wear.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartr.wear.worker.PassiveRegistrationWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val work = OneTimeWorkRequestBuilder<PassiveRegistrationWorker>().build()
        WorkManager.getInstance(context).enqueue(work)
    }
}
