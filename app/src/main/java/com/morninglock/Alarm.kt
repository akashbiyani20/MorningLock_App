package com.morninglock

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Data Model ───────────────────────────────────────────────────────────────

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isPrimary: Boolean = false,       // Orange — triggers lockdown on stop
    val isEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,           // snooze duration
    val lockDurationMinutes: Int = 30,    // only used if isPrimary = true
    val vibrate: Boolean = true,
    val ringtoneUri: String = "default",  // "default" = system default alarm
    // Days: bitmask — 0=one-time, 1=Sun,2=Mon,4=Tue,8=Wed,16=Thu,32=Fri,64=Sat
    val repeatDays: Int = 0
) {
    fun daysLabel(): String {
        if (repeatDays == 0) return "Once"
        if (repeatDays == 127) return "Every day"
        val days = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        return (0..6).filter { repeatDays and (1 shl it) != 0 }.joinToString(", ") { days[it] }
    }

    fun timeLabel(): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(h, minute, amPm)
    }
}

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY isPrimary DESC, hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarm(id: Int): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<Alarm>
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "morninglock.db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}
