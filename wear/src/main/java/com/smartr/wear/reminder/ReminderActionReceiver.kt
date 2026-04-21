package com.smartr.wear.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.smartr.wear.data.history.HistoryRepository
import java.time.LocalDate
import java.time.ZoneId

class ReminderActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ACKNOWLEDGE = "com.smartr.wear.ACTION_ACKNOWLEDGE_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ACKNOWLEDGE) return
        CoroutineScope(Dispatchers.IO).launch {
            HistoryRepository(context.applicationContext).recordReminderAcknowledged(
                LocalDate.now(ZoneId.systemDefault())
            )
        }
    }
}
