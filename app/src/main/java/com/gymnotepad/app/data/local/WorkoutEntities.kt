package com.gymnotepad.app.data.local

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseDefinitionId: String?,
    val name: String,
    val sortOrder: Int,
    val loadMode: String,
    val repMin: Int,
    val repMax: Int,
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val loadMode: String,
    @ColumnInfo(defaultValue = "'CUSTOM_VALUES'") val progressionType: String,
    val barWeightGrams: Long?,
    val createdAt: Long,
)

@Entity(
    tableName = "exercise_load_options",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exerciseId", "loadGrams"], unique = true)],
)
data class ExerciseLoadOptionEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val loadGrams: Long,
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
)

data class TemplateExerciseRow(
    val templateExerciseId: String,
    val templateId: String,
    val exerciseId: String,
    val exerciseName: String,
    val loadMode: String,
    val sortOrder: Int,
    val workSetCount: Int,
    val repMin: Int,
    val repMax: Int,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val sortOrder: Int,
    val loadGrams: Long?,
    val reps: Int?,
    val effort: String,
    val completed: Boolean,
    val completedAt: Long?,
)

@Entity(
    tableName = "body_weight_measurements",
    indices = [Index("measuredAt")],
)
data class BodyWeightMeasurementEntity(
    @PrimaryKey val id: String,
    val measuredAt: Long,
    val weightGrams: Long,
)

data class ExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionExerciseId",
    )
    val sets: List<WorkoutSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<ExerciseWithSets>,
)
