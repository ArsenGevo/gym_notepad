package com.gymnotepad.app.data

import androidx.room.withTransaction
import com.gymnotepad.app.data.local.GymDatabase
import com.gymnotepad.app.data.local.BodyWeightMeasurementEntity
import com.gymnotepad.app.data.local.ExerciseEntity
import com.gymnotepad.app.data.local.ExerciseLoadOptionEntity
import com.gymnotepad.app.data.local.SessionExerciseEntity
import com.gymnotepad.app.data.local.SessionWithExercises
import com.gymnotepad.app.data.local.TemplateExerciseEntity
import com.gymnotepad.app.data.local.TemplateExerciseRow
import com.gymnotepad.app.data.local.WorkoutSessionEntity
import com.gymnotepad.app.data.local.WorkoutSetEntity
import com.gymnotepad.app.data.local.WorkoutTemplateEntity
import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.LoadProgressionType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(
    private val database: GymDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : WorkoutRepository {
    private val dao = database.workoutDao()
    private val templateDao = database.templateDao()
    private val bodyWeightDao = database.bodyWeightDao()

    override fun observeActiveWorkout(): Flow<ActiveWorkout?> =
        dao.observeActiveWorkout().map { it?.toDomain() }

    override fun observeHistory(): Flow<List<CompletedWorkout>> =
        dao.observeCompletedWorkouts().map { sessions -> sessions.map(SessionWithExercises::toCompletedWorkout) }

    override fun observeExercises(): Flow<List<ExerciseDefinition>> = combine(
        templateDao.observeExercises(),
        templateDao.observeLoadOptions(),
    ) { exercises, options ->
        exercises.map { exercise ->
            ExerciseDefinition(
                id = exercise.id,
                name = exercise.name,
                loadMode = LoadMode.valueOf(exercise.loadMode),
                progressionType = LoadProgressionType.valueOf(exercise.progressionType),
                availableLoadsGrams = options.filter { it.exerciseId == exercise.id }.map { it.loadGrams },
                barWeightGrams = exercise.barWeightGrams,
            )
        }
    }

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> = combine(
        templateDao.observeTemplates(),
        templateDao.observeTemplateExerciseRows(),
    ) { templates, rows ->
        templates.map { template ->
            WorkoutTemplate(
                id = template.id,
                name = template.name,
                exercises = rows.filter { it.templateId == template.id }.map(TemplateExerciseRow::toDomain),
            )
        }
    }

    override fun observeBodyWeight(): Flow<List<BodyWeightMeasurement>> =
        bodyWeightDao.observeMeasurements().map { measurements ->
            measurements.map { measurement ->
                BodyWeightMeasurement(
                    id = measurement.id,
                    measuredAt = measurement.measuredAt,
                    weightGrams = measurement.weightGrams,
                )
            }
        }

    override suspend fun ensureStarterData() {
        database.withTransaction {
            val createdAt = now()
            templateDao.insertExercises(
                listOf(
                    ExerciseEntity(
                        "starter-incline-dumbbell-press",
                        "Жим гантелей на наклонной",
                        LoadMode.DUMBBELL_PER_HAND.name,
                        LoadProgressionType.DUMBBELL_2_KG.name,
                        null,
                        createdAt,
                    ),
                    ExerciseEntity(
                        "starter-lat-pulldown",
                        "Тяга верхнего блока",
                        LoadMode.MACHINE.name,
                        LoadProgressionType.CUSTOM_VALUES.name,
                        null,
                        createdAt,
                    ),
                    ExerciseEntity(
                        "starter-squat",
                        "Приседания со штангой",
                        LoadMode.EXTERNAL_TOTAL.name,
                        LoadProgressionType.BARBELL_PLATES.name,
                        20_000,
                        createdAt,
                    ),
                    ExerciseEntity(
                        "starter-pull-up",
                        "Подтягивания",
                        LoadMode.BODYWEIGHT.name,
                        LoadProgressionType.NONE.name,
                        null,
                        createdAt,
                    ),
                ),
            )
            templateDao.insertTemplate(WorkoutTemplateEntity("starter-template-a", "Тренировка A", createdAt))
            templateDao.insertTemplateExercises(
                listOf(
                    TemplateExerciseEntity(
                        id = "starter-template-a-press",
                        templateId = "starter-template-a",
                        exerciseId = "starter-incline-dumbbell-press",
                        sortOrder = 0,
                        workSetCount = 3,
                        repMin = 8,
                        repMax = 12,
                    ),
                ),
            )
        }
    }

    override suspend fun createExercise(
        name: String,
        loadMode: LoadMode,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    ) {
        val normalized = name.trim()
        if (normalized.isEmpty()) return
        database.withTransaction {
            val exerciseId = UUID.randomUUID().toString()
            templateDao.insertExercises(
                listOf(
                    ExerciseEntity(
                        id = exerciseId,
                        name = normalized,
                        loadMode = loadMode.name,
                        progressionType = progressionType.name,
                        barWeightGrams = barWeightGrams,
                        createdAt = now(),
                    ),
                ),
            )
            templateDao.insertLoadOptions(
                availableLoadsGrams.distinct().sorted().map { load ->
                    ExerciseLoadOptionEntity(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        loadGrams = load,
                    )
                },
            )
        }
    }

    override suspend fun createTemplate(name: String) {
        val normalized = name.trim()
        if (normalized.isEmpty()) return
        templateDao.insertTemplate(WorkoutTemplateEntity(UUID.randomUUID().toString(), normalized, now()))
    }

    override suspend fun updateExerciseLoadConfiguration(
        exerciseId: String,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    ) {
        database.withTransaction {
            val exercise = templateDao.getExercise(exerciseId) ?: return@withTransaction
            templateDao.updateExercise(
                exercise.copy(
                    progressionType = progressionType.name,
                    barWeightGrams = barWeightGrams,
                ),
            )
            templateDao.deleteLoadOptions(exerciseId)
            templateDao.insertLoadOptions(
                availableLoadsGrams.distinct().sorted().map { load ->
                    ExerciseLoadOptionEntity(UUID.randomUUID().toString(), exerciseId, load)
                },
            )
        }
    }

    override suspend fun addExerciseToTemplate(templateId: String, exerciseId: String) {
        templateDao.insertTemplateExercises(
            listOf(
                TemplateExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    templateId = templateId,
                    exerciseId = exerciseId,
                    sortOrder = templateDao.nextSortOrder(templateId),
                    workSetCount = 3,
                    repMin = 8,
                    repMax = 12,
                ),
            ),
        )
    }

    override suspend fun updateTemplateExercise(
        templateExerciseId: String,
        workSetCount: Int,
        repMin: Int,
        repMax: Int,
    ) {
        val current = templateDao.getTemplateExercise(templateExerciseId) ?: return
        templateDao.updateTemplateExercise(
            current.copy(
                workSetCount = workSetCount.coerceAtLeast(1),
                repMin = repMin.coerceAtLeast(1).coerceAtMost(repMax.coerceAtLeast(1)),
                repMax = repMax.coerceAtLeast(repMin.coerceAtLeast(1)),
            ),
        )
    }

    override suspend fun startWorkoutFromTemplate(templateId: String): String = database.withTransaction {
        dao.getActiveSession()?.id ?: createFromTemplate(templateId)
    }

    override suspend fun startWorkoutFromPrevious(sessionId: String): String = database.withTransaction {
        dao.getActiveSession()?.id ?: dao.getLatestCompletedWorkout()
            ?.let { previous -> cloneWorkout(previous) }
            ?: createSeededWorkout("Тренировка A")
    }

    override suspend fun startEmptyWorkout(name: String): String = database.withTransaction {
        dao.getActiveSession()?.id ?: createSession(name.ifBlank { "Новая тренировка" })
    }

    override suspend fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String) {
        database.withTransaction {
            val activeSession = dao.getActiveSession()?.takeIf { it.id == sessionId } ?: return@withTransaction
            val definition = templateDao.getExercise(exerciseId) ?: return@withTransaction
            val sessionExerciseId = UUID.randomUUID().toString()
            dao.insertExercises(
                listOf(
                    SessionExerciseEntity(
                        id = sessionExerciseId,
                        sessionId = activeSession.id,
                        exerciseDefinitionId = definition.id,
                        name = definition.name,
                        sortOrder = dao.nextExerciseSortOrder(activeSession.id),
                        loadMode = definition.loadMode,
                        repMin = 8,
                        repMax = 12,
                    ),
                ),
            )
            dao.insertSets(newBlankSets(sessionExerciseId, 3))
        }
    }

    override suspend fun replaceActiveExercise(sessionExerciseId: String, exerciseId: String) {
        database.withTransaction {
            val current = dao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            val activeSession = dao.getActiveSession()?.takeIf { it.id == current.sessionId } ?: return@withTransaction
            val definition = templateDao.getExercise(exerciseId) ?: return@withTransaction
            dao.updateSessionExercise(
                current.copy(
                    exerciseDefinitionId = definition.id,
                    name = definition.name,
                    loadMode = definition.loadMode,
                ),
            )
        }
    }

    override suspend fun removeExerciseFromActiveWorkout(sessionExerciseId: String) {
        database.withTransaction {
            val current = dao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            dao.getActiveSession()?.takeIf { it.id == current.sessionId } ?: return@withTransaction
            dao.deleteSessionExercise(sessionExerciseId)
            dao.getSessionExercises(current.sessionId).forEachIndexed { index, exercise ->
                if (exercise.sortOrder != index) dao.updateSessionExercise(exercise.copy(sortOrder = index))
            }
        }
    }

    override suspend fun moveActiveExercise(sessionExerciseId: String, direction: Int) {
        if (direction == 0) return
        database.withTransaction {
            val current = dao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            dao.getActiveSession()?.takeIf { it.id == current.sessionId } ?: return@withTransaction
            val exercises = dao.getSessionExercises(current.sessionId)
            val currentIndex = exercises.indexOfFirst { it.id == sessionExerciseId }
            val targetIndex = currentIndex + direction.coerceIn(-1, 1)
            if (currentIndex < 0 || targetIndex !in exercises.indices) return@withTransaction
            val target = exercises[targetIndex]
            dao.updateSessionExercise(current.copy(sortOrder = target.sortOrder))
            dao.updateSessionExercise(target.copy(sortOrder = current.sortOrder))
        }
    }

    override suspend fun addSetToActiveExercise(sessionExerciseId: String) {
        database.withTransaction {
            val exercise = dao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            dao.getActiveSession()?.takeIf { it.id == exercise.sessionId } ?: return@withTransaction
            dao.insertSets(newBlankSets(sessionExerciseId, 1, dao.nextSetSortOrder(sessionExerciseId)))
        }
    }

    override suspend fun removeLastSetFromActiveExercise(sessionExerciseId: String) {
        database.withTransaction {
            val exercise = dao.getSessionExercise(sessionExerciseId) ?: return@withTransaction
            dao.getActiveSession()?.takeIf { it.id == exercise.sessionId } ?: return@withTransaction
            if (dao.getSetCount(sessionExerciseId) <= 1) return@withTransaction
            dao.getLastSet(sessionExerciseId)?.let { dao.deleteSet(it.id) }
        }
    }

    override suspend fun saveSet(input: SetInput) {
        database.withTransaction {
            val current = dao.getSet(input.setId) ?: return@withTransaction
            dao.updateSet(
                current.copy(
                    loadGrams = input.loadGrams,
                    reps = input.reps,
                    effort = input.effort.name,
                    completed = input.completed,
                    completedAt = if (input.completed) now() else null,
                ),
            )
        }
    }

    override suspend fun completeWorkout(sessionId: String) {
        dao.completeWorkout(sessionId, now())
    }

    override suspend fun saveBodyWeight(weightGrams: Long) {
        if (weightGrams !in 20_000L..500_000L) return
        bodyWeightDao.insertMeasurement(
            BodyWeightMeasurementEntity(
                id = UUID.randomUUID().toString(),
                measuredAt = now(),
                weightGrams = weightGrams,
            ),
        )
    }

    override suspend fun deleteBodyWeight(measurementId: String) {
        bodyWeightDao.deleteMeasurement(measurementId)
    }

    private suspend fun startSeededWorkout(name: String): String = database.withTransaction {
        dao.getActiveSession()?.id ?: createSeededWorkout(name)
    }

    private suspend fun createSeededWorkout(name: String): String {
        val sessionId = createSession(name)
        val exerciseId = UUID.randomUUID().toString()
        dao.insertExercises(
            listOf(
                SessionExerciseEntity(
                    id = exerciseId,
                    sessionId = sessionId,
                    exerciseDefinitionId = null,
                    name = "Жим гантелей на наклонной",
                    sortOrder = 0,
                    loadMode = LoadMode.DUMBBELL_PER_HAND.name,
                    repMin = 8,
                    repMax = 12,
                ),
            ),
        )
        dao.insertSets(
            (0 until 3).map { index ->
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = exerciseId,
                    sortOrder = index,
                    loadGrams = null,
                    reps = null,
                    effort = EffortMark.UNKNOWN.name,
                    completed = false,
                    completedAt = null,
                )
            },
        )
        return sessionId
    }

    private suspend fun cloneWorkout(previous: SessionWithExercises): String {
        val newSessionId = createSession("Повтор: ${previous.session.name}")
        previous.exercises.sortedBy { it.exercise.sortOrder }.forEach { previousExercise ->
            val newExerciseId = UUID.randomUUID().toString()
            dao.insertExercises(
                listOf(
                    previousExercise.exercise.copy(
                        id = newExerciseId,
                        sessionId = newSessionId,
                    ),
                ),
            )
            dao.insertSets(
                previousExercise.sets.sortedBy { it.sortOrder }.map { previousSet ->
                    previousSet.copy(
                        id = UUID.randomUUID().toString(),
                        sessionExerciseId = newExerciseId,
                        reps = null,
                        effort = EffortMark.UNKNOWN.name,
                        completed = false,
                        completedAt = null,
                    )
                },
            )
        }
        return newSessionId
    }

    private suspend fun createFromTemplate(templateId: String): String {
        val template = templateDao.getTemplate(templateId)
            ?: return createSeededWorkout("Тренировка A")
        val rows = templateDao.getTemplateExerciseRows(templateId)
        val sessionId = createSession(template.name)
        rows.forEach { row ->
            val sessionExerciseId = UUID.randomUUID().toString()
            dao.insertExercises(
                listOf(
                    SessionExerciseEntity(
                        id = sessionExerciseId,
                        sessionId = sessionId,
                        exerciseDefinitionId = row.exerciseId,
                        name = row.exerciseName,
                        sortOrder = row.sortOrder,
                        loadMode = row.loadMode,
                        repMin = row.repMin,
                        repMax = row.repMax,
                    ),
                ),
            )
            dao.insertSets(
                (0 until row.workSetCount).map { index ->
                    WorkoutSetEntity(
                        id = UUID.randomUUID().toString(),
                        sessionExerciseId = sessionExerciseId,
                        sortOrder = index,
                        loadGrams = null,
                        reps = null,
                        effort = EffortMark.UNKNOWN.name,
                        completed = false,
                        completedAt = null,
                    )
                },
            )
        }
        return sessionId
    }

    private suspend fun createSession(name: String): String {
        val id = UUID.randomUUID().toString()
        dao.insertSession(
            WorkoutSessionEntity(
                id = id,
                name = name,
                status = "ACTIVE",
                startedAt = now(),
                completedAt = null,
            ),
        )
        return id
    }

    private fun newBlankSets(exerciseId: String, count: Int, startOrder: Int = 0): List<WorkoutSetEntity> =
        (startOrder until startOrder + count).map { index ->
            WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                sessionExerciseId = exerciseId,
                sortOrder = index,
                loadGrams = null,
                reps = null,
                effort = EffortMark.UNKNOWN.name,
                completed = false,
                completedAt = null,
            )
        }
}

private fun SessionWithExercises.toDomain(): ActiveWorkout = ActiveWorkout(
    id = session.id,
    name = session.name,
    startedAt = session.startedAt,
    exercises = exercises
        .sortedBy { it.exercise.sortOrder }
        .map { relation ->
            WorkoutExercise(
                id = relation.exercise.id,
                definitionId = relation.exercise.exerciseDefinitionId,
                name = relation.exercise.name,
                loadMode = LoadMode.valueOf(relation.exercise.loadMode),
                repMin = relation.exercise.repMin,
                repMax = relation.exercise.repMax,
                sets = relation.sets
                    .sortedBy { it.sortOrder }
                    .map { set ->
                        WorkoutSet(
                            id = set.id,
                            order = set.sortOrder,
                            loadGrams = set.loadGrams,
                            reps = set.reps,
                            effort = EffortMark.valueOf(set.effort),
                            completed = set.completed,
                        )
                    },
            )
        },
)

private fun SessionWithExercises.toCompletedWorkout(): CompletedWorkout = CompletedWorkout(
    id = session.id,
    name = session.name,
    startedAt = session.startedAt,
    completedAt = requireNotNull(session.completedAt),
    exercises = exercises.sortedBy { it.exercise.sortOrder }.map { relation ->
        WorkoutExercise(
            id = relation.exercise.id,
            definitionId = relation.exercise.exerciseDefinitionId,
            name = relation.exercise.name,
            loadMode = LoadMode.valueOf(relation.exercise.loadMode),
            repMin = relation.exercise.repMin,
            repMax = relation.exercise.repMax,
            sets = relation.sets.sortedBy { it.sortOrder }.map { set ->
                WorkoutSet(
                    id = set.id,
                    order = set.sortOrder,
                    loadGrams = set.loadGrams,
                    reps = set.reps,
                    effort = EffortMark.valueOf(set.effort),
                    completed = set.completed,
                )
            },
        )
    },
)

private fun TemplateExerciseRow.toDomain(): TemplateExercise = TemplateExercise(
    id = templateExerciseId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    loadMode = LoadMode.valueOf(loadMode),
    workSetCount = workSetCount,
    repMin = repMin,
    repMax = repMax,
)
