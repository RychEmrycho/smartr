package com.smartr.data.history

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class DailySummary(
    @PrimaryKey val dateIso: String,
    val sedentaryMinutes: Int,
    val remindersSent: Int,
    val remindersAcknowledged: Int
)

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummary)

    @Query("SELECT * FROM DailySummary WHERE dateIso = :dateIso LIMIT 1")
    suspend fun findByDate(dateIso: String): DailySummary?

    @Query("UPDATE DailySummary SET remindersSent = remindersSent + 1 WHERE dateIso = :dateIso")
    suspend fun incrementSent(dateIso: String)

    @Query("UPDATE DailySummary SET remindersAcknowledged = remindersAcknowledged + 1 WHERE dateIso = :dateIso")
    suspend fun incrementAcknowledged(dateIso: String)

    @Query("UPDATE DailySummary SET sedentaryMinutes = sedentaryMinutes + :minutes WHERE dateIso = :dateIso")
    suspend fun addMinutes(dateIso: String, minutes: Int)

    @Query("SELECT * FROM DailySummary ORDER BY dateIso DESC LIMIT 30")
    fun latest30Days(): Flow<List<DailySummary>>
}

@Database(entities = [DailySummary::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun get(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "smartr_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
