package com.smartr.wear.complication

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object ComplicationUpdater {
    fun updateAll(context: Context) {
        val wellnessComponent = ComponentName(context, WellnessComplicationService::class.java)
        val streakComponent = ComponentName(context, StreakComplicationService::class.java)
        
        val requester = ComplicationDataSourceUpdateRequester.create(context, wellnessComponent)
        requester.requestUpdateAll()
        
        val streakRequester = ComplicationDataSourceUpdateRequester.create(context, streakComponent)
        streakRequester.requestUpdateAll()
    }
}
