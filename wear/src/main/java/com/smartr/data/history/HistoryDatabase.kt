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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Instant

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

enum class EventType {
    SEDENTARY_START, SEDENTARY_STOPPED, REMINDER_SENT, SEDENTARY_RESET
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String, // ISO-8601 / RFC3339
    val type: EventType,
    val sessionId: String?, // UUID to group session related events
    val metadata: Map<String, String>? // Map converted to JSON string
)

class Converters {
    private val json = Json

    @TypeConverter
    fun fromList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<Int> = 
        if (value.isEmpty()) List(24) { 0 } 
        else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = 
        try { EventType.valueOf(value) } catch (e: Exception) { EventType.SEDENTARY_START }

    @TypeConverter
    fun fromMetadata(value: Map<String, String>?): String? = 
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toMetadata(value: String?): Map<String, String>? = 
        value?.let { try { json.decodeFromString<Map<String, String>>(it) } catch (e: Exception) { null } }
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

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Query("SELECT * FROM events WHERE timestamp >= :dayStart AND timestamp <= :dayEnd ORDER BY timestamp DESC")
    fun eventsForRange(dayStart: String, dayEnd: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE type = :type AND sessionId IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSessionEvent(type: EventType = EventType.SEDENTARY_START): Event?

    @Update
    suspend fun update(event: Event)

    @Query("DELETE FROM events")
    suspend fun clearAll()

    @Query("DELETE FROM events WHERE timestamp LIKE :dateIso || '%'")
    suspend fun deleteForDay(dateIso: String)
}

@Database(entities = [DailySummary::class, PersonalBest::class, Event::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun personalBestDao(): PersonalBestDao
    abstract fun eventDao(): EventDao

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
