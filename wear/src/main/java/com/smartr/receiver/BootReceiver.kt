package com.smartr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartr.worker.PassiveRegistrationWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val work = OneTimeWorkRequestBuilder<PassiveRegistrationWorker>().build()
        WorkManager.getInstance(context).enqueue(work)

        val offBodyIntent = Intent(context, com.smartr.service.OffBodyService::class.java)
        context.startService(offBodyIntent)
    }
}
