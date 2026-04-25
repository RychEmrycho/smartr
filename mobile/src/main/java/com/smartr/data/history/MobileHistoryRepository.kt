package com.smartr.data.history

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MobileHistoryRepository(context: Context) {
    private val dao = HistoryDatabase.getInstance(context).historyDao()

    fun summaries(limit: Int = 365): Flow<List<DailySummaryEntity>> = 
        dao.getRecentSummaries(limit)

    suspend fun recordSummary(summary: DailySummaryEntity) {
        dao.upsert(summary)
    }

    suspend fun recordSummaries(summaries: List<DailySummaryEntity>) {
        dao.upsertAll(summaries)
    }
}
