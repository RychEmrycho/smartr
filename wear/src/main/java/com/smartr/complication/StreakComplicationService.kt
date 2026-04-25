package com.smartr.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.smartr.MainActivity
import com.smartr.data.history.HistoryRepository
import com.smartr.logic.BehaviorInsightsEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class StreakComplicationService : ComplicationDataSourceService() {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val historyRepository = HistoryRepository(applicationContext)
        val engine = BehaviorInsightsEngine()

        scope.launch {
            val summaries = historyRepository.summaries().first()
            val snapshot = engine.build(summaries)
            val streak = snapshot.currentStreak

            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val complicationData = when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder("🔥 $streak").build(),
                        contentDescription = PlainComplicationText.Builder("Current Streak").build()
                    )
                        .setTapAction(pendingIntent)
                        .build()
                }
                else -> null
            }
            listener.onComplicationData(complicationData)
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("🔥 5").build(),
                    contentDescription = PlainComplicationText.Builder("Current Streak").build()
                ).build()
            }
            else -> null
        }
    }
}
