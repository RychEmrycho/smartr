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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity
data class DailySummary(
    @PrimaryKey val dateIso: String,
    val sedentarySeconds: Int,
    val remindersSent: Int,
    val remindersAcknowledged: Int,
    val hourlySedentarySeconds: List<Int> = List(24) { 0 },
    val sedentaryThresholdSeconds: Int = 2700 // Default to 45 minutes
)

@Entity
data class PersonalBest(
    @PrimaryKey val recordType: String, // e.g., "max_streak", "min_sedentary", "max_response_rate"
    val value: Int,
    val dateIso: String
)

class Converters {
    @TypeConverter
    fun fromList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<Int> = 
        if (value.isEmpty()) List(24) { 0 } 
        else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromEventType(value: SedentaryEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): SedentaryEventType = 
        try { SedentaryEventType.valueOf(value) } catch (e: Exception) { SedentaryEventType.START }
}

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

    @Query("UPDATE DailySummary SET sedentarySeconds = sedentarySeconds + :seconds WHERE dateIso = :dateIso")
    suspend fun addSeconds(dateIso: String, seconds: Int)

    @Query("SELECT * FROM DailySummary ORDER BY dateIso DESC LIMIT 30")
    fun latest30Days(): Flow<List<DailySummary>>
}

@Dao
interface PersonalBestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pb: PersonalBest)

    @Query("SELECT * FROM PersonalBest")
    fun getAll(): Flow<List<PersonalBest>>

    @Query("SELECT * FROM PersonalBest WHERE recordType = :type LIMIT 1")
    suspend fun findByType(type: String): PersonalBest?
}

enum class SedentaryEventType {
    START, STOPPED, REMINDER_SENT, RESET
}

@Entity
data class SedentaryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val type: SedentaryEventType,
    val durationSeconds: Int = 0,
    val metadata: String? = null
)

@Dao
interface SedentaryEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SedentaryEvent): Long

    @Query("SELECT * FROM SedentaryEvent WHERE dateIso = :dateIso ORDER BY startTimeMillis ASC")
    fun eventsForDay(dateIso: String): Flow<List<SedentaryEvent>>

    @Query("SELECT * FROM SedentaryEvent WHERE endTimeMillis IS NULL AND type = :type ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveEvent(type: SedentaryEventType = SedentaryEventType.START): SedentaryEvent?

    @Update
    suspend fun update(event: SedentaryEvent)

    @Query("DELETE FROM SedentaryEvent")
    suspend fun clearAll()

    @Query("DELETE FROM SedentaryEvent WHERE dateIso = :dateIso")
    suspend fun deleteForDay(dateIso: String)
}

@Database(entities = [DailySummary::class, PersonalBest::class, SedentaryEvent::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun personalBestDao(): PersonalBestDao
    abstract fun sedentaryEventDao(): SedentaryEventDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun get(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "smartr_history.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
