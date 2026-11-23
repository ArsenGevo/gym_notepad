package com.gymnotepad.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_load_options ORDER BY loadGrams")
    fun observeLoadOptions(): Flow<List<ExerciseLoadOptionEntity>>

    @Query("SELECT * FROM workout_templates ORDER BY createdAt, name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query(
        """
        SELECT te.id AS templateExerciseId, te.templateId, te.exerciseId,
               e.name AS exerciseName, e.loadMode, te.sortOrder,
               te.workSetCount, te.repMin, te.repMax
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
        ORDER BY te.templateId, te.sortOrder
        """,
    )
    fun observeTemplateExerciseRows(): Flow<List<TemplateExerciseRow>>

    @Query("SELECT * FROM workout_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplate(templateId: String): WorkoutTemplateEntity?

    @Query(
        """
        SELECT te.id AS templateExerciseId, te.templateId, te.exerciseId,
               e.name AS exerciseName, e.loadMode, te.sortOrder,
               te.workSetCount, te.repMin, te.repMax
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
        WHERE te.templateId = :templateId
        ORDER BY te.sortOrder
        """,
    )
    suspend fun getTemplateExerciseRows(templateId: String): List<TemplateExerciseRow>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM template_exercises WHERE templateId = :templateId")
    suspend fun nextSortOrder(templateId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLoadOptions(options: List<ExerciseLoadOptionEntity>)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExercise(exerciseId: String): ExerciseEntity?

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercise_load_options WHERE exerciseId = :exerciseId")
    suspend fun deleteLoadOptions(exerciseId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    @Update
    suspend fun updateTemplateExercise(exercise: TemplateExerciseEntity)

    @Query("SELECT * FROM template_exercises WHERE id = :id LIMIT 1")
    suspend fun getTemplateExercise(id: String): TemplateExerciseEntity?
}
