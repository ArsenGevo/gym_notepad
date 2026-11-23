package com.gymnotepad.app.data

import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.LoadProgressionType
import kotlinx.coroutines.flow.Flow

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val loadMode: LoadMode,
    val progressionType: LoadProgressionType,
    val availableLoadsGrams: List<Long>,
    val barWeightGrams: Long?,
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val exercises: List<TemplateExercise>,
)

data class TemplateExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val loadMode: LoadMode,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
)

data class ActiveWorkout(
    val id: String,
    val name: String,
    val startedAt: Long,
    val exercises: List<WorkoutExercise>,
)

data class CompletedWorkout(
    val id: String,
    val name: String,
    val startedAt: Long,
    val completedAt: Long,
    val exercises: List<WorkoutExercise>,
)

data class WorkoutExercise(
    val id: String,
    val definitionId: String?,
    val name: String,
    val loadMode: LoadMode,
    val repMin: Int,
    val repMax: Int,
    val sets: List<WorkoutSet>,
)

data class WorkoutSet(
    val id: String,
    val order: Int,
    val loadGrams: Long?,
    val reps: Int?,
    val effort: EffortMark,
    val completed: Boolean,
)

data class SetInput(
    val setId: String,
    val loadGrams: Long?,
    val reps: Int?,
    val effort: EffortMark,
    val completed: Boolean = true,
)

data class BodyWeightMeasurement(
    val id: String,
    val measuredAt: Long,
    val weightGrams: Long,
)

interface WorkoutRepository {
    fun observeActiveWorkout(): Flow<ActiveWorkout?>
    fun observeHistory(): Flow<List<CompletedWorkout>>
    fun observeExercises(): Flow<List<ExerciseDefinition>>
    fun observeTemplates(): Flow<List<WorkoutTemplate>>
    fun observeBodyWeight(): Flow<List<BodyWeightMeasurement>>
    suspend fun ensureStarterData()
    suspend fun createExercise(
        name: String,
        loadMode: LoadMode,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    )
    suspend fun updateExerciseLoadConfiguration(
        exerciseId: String,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    )
    suspend fun createTemplate(name: String)
    suspend fun addExerciseToTemplate(templateId: String, exerciseId: String)
    suspend fun updateTemplateExercise(
        templateExerciseId: String,
        workSetCount: Int,
        repMin: Int,
        repMax: Int,
    )
    suspend fun startWorkoutFromTemplate(templateId: String): String
    suspend fun startWorkoutFromPrevious(sessionId: String): String
    suspend fun startEmptyWorkout(name: String): String
    suspend fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String)
    suspend fun replaceActiveExercise(sessionExerciseId: String, exerciseId: String)
    suspend fun removeExerciseFromActiveWorkout(sessionExerciseId: String)
    suspend fun moveActiveExercise(sessionExerciseId: String, direction: Int)
    suspend fun addSetToActiveExercise(sessionExerciseId: String)
    suspend fun removeLastSetFromActiveExercise(sessionExerciseId: String)
    suspend fun saveSet(input: SetInput)
    suspend fun completeWorkout(sessionId: String)
    suspend fun saveBodyWeight(weightGrams: Long)
    suspend fun deleteBodyWeight(measurementId: String)
}

