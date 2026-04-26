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

import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity
data class DailySummary(
    @PrimaryKey val dateIso: String,
    val sedentaryMinutes: Int,
    val remindersSent: Int,
    val remindersAcknowledged: Int,
    val hourlySedentary: List<Int> = List(24) { 0 }
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

    @Query("UPDATE DailySummary SET sedentaryMinutes = sedentaryMinutes + :minutes WHERE dateIso = :dateIso")
    suspend fun addMinutes(dateIso: String, minutes: Int)

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

@Database(entities = [DailySummary::class, PersonalBest::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun personalBestDao(): PersonalBestDao

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
