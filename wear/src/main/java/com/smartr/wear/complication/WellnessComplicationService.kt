package com.smartr.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.smartr.wear.MainActivity
import kotlinx.coroutines.launch
import com.smartr.wear.data.history.HistoryRepository
import com.smartr.wear.logic.BehaviorInsightsEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class WellnessComplicationService : ComplicationDataSourceService() {
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
            val score = snapshot.wellnessScore.toFloat()

            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val complicationData = when (request.complicationType) {
                ComplicationType.RANGED_VALUE -> {
                    RangedValueComplicationData.Builder(
                        value = score,
                        min = 0f,
                        max = 100f,
                        contentDescription = PlainComplicationText.Builder("Wellness Score").build()
                    )
                        .setTapAction(pendingIntent)
                        .setText(PlainComplicationText.Builder(score.toInt().toString()).build())
                        .build()
                }
                else -> null
            }
            listener.onComplicationData(complicationData)
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 85f,
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder("Wellness Score").build()
                )
                    .setText(PlainComplicationText.Builder("85").build())
                    .build()
            }
            else -> null
        }
    }
}
