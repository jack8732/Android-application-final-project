package com.example.fitpick.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val muscles: String,
    val level: String,
    val minutes: Int,
    val goal: String,
    val exercisesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val muscles: String,
    val completedExercises: Int,
    val totalExercises: Int,
    val durationSec: Int,
    val completedAt: Long = System.currentTimeMillis()
)

@Dao
interface FitDao {
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun routines(): Flow<List<RoutineEntity>>

    @Insert
    suspend fun insertRoutine(r: RoutineEntity): Long

    @Delete
    suspend fun deleteRoutine(r: RoutineEntity)

    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC")
    fun logs(): Flow<List<WorkoutLogEntity>>

    @Insert
    suspend fun insertLog(l: WorkoutLogEntity)

    @Query("DELETE FROM workout_logs")
    suspend fun clearLogs()
}

@Database(entities = [RoutineEntity::class, WorkoutLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FitDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "fitpick.db"
                ).build().also { instance = it }
            }
    }
}
