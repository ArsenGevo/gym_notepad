package com.gymnotepad.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedWorkouts(): Flow<List<SessionWithExercises>>

    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLatestCompletedWorkout(): SessionWithExercises?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercises(exercises: List<SessionExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("SELECT * FROM session_exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getSessionExercise(exerciseId: String): SessionExerciseEntity?

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    suspend fun getSessionExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun nextExerciseSortOrder(sessionId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM workout_sets WHERE sessionExerciseId = :exerciseId")
    suspend fun nextSetSortOrder(exerciseId: String): Int

    @Query("SELECT * FROM workout_sets WHERE sessionExerciseId = :exerciseId ORDER BY sortOrder DESC LIMIT 1")
    suspend fun getLastSet(exerciseId: String): WorkoutSetEntity?

    @Query("SELECT COUNT(*) FROM workout_sets WHERE sessionExerciseId = :exerciseId")
    suspend fun getSetCount(exerciseId: String): Int

    @Update
    suspend fun updateSessionExercise(exercise: SessionExerciseEntity)

    @Query("DELETE FROM session_exercises WHERE id = :exerciseId")
    suspend fun deleteSessionExercise(exerciseId: String)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getSet(setId: String): WorkoutSetEntity?

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("UPDATE workout_sessions SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :sessionId AND status = 'ACTIVE'")
    suspend fun completeWorkout(sessionId: String, completedAt: Long): Int
}
