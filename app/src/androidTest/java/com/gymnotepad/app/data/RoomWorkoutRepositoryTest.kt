package com.gymnotepad.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymnotepad.app.data.local.GymDatabase
import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RoomWorkoutRepositoryTest {
    private lateinit var database: GymDatabase
    private lateinit var repository: RoomWorkoutRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomWorkoutRepository(database) { 1_000L }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun workoutIsCreatedUpdatedRestoredCompletedAndRepeated() = runBlocking {
        repository.ensureStarterData()
        val template = repository.observeTemplates().first { it.isNotEmpty() }.single()
        assertEquals("Тренировка A", template.name)

        val sessionId = repository.startWorkoutFromTemplate(template.id)
        val created = repository.observeActiveWorkout().filterNotNull().first()
        val setId = created.exercises.single().sets.first().id

        repository.saveSet(
            SetInput(
                setId = setId,
                loadGrams = 32_000,
                reps = 10,
                effort = EffortMark.RIR_1,
            ),
        )

        val restored = repository.observeActiveWorkout()
            .filter { workout ->
                workout?.exercises?.single()?.sets?.first()?.completed == true
            }
            .filterNotNull()
            .first()
        val savedSet = restored.exercises.single().sets.first()
        assertEquals(32_000L, savedSet.loadGrams)
        assertEquals(10, savedSet.reps)
        assertEquals(EffortMark.RIR_1, savedSet.effort)

        repository.completeWorkout(sessionId)

        assertNull(repository.observeActiveWorkout().first())
        val completed = repository.observeHistory().first { it.size == 1 }.single()
        assertEquals("Тренировка A", completed.name)
        assertEquals(32_000L, completed.exercises.single().sets.first().loadGrams)

        repository.startWorkoutFromPrevious(sessionId)
        val repeated = repository.observeActiveWorkout().filterNotNull().first()
        val repeatedSet = repeated.exercises.single().sets.first()
        assertEquals(32_000L, repeatedSet.loadGrams)
        assertNull(repeatedSet.reps)
        assertEquals(EffortMark.UNKNOWN, repeatedSet.effort)
    }

    @Test
    fun customTemplateCreatesWorkoutWithConfiguredPrescription() = runBlocking {
        repository.ensureStarterData()
        repository.createExercise(
            "Румынская тяга",
            LoadMode.EXTERNAL_TOTAL,
            com.gymnotepad.app.domain.LoadProgressionType.BARBELL_PLATES,
            emptyList(),
            20_000,
        )
        repository.createTemplate("Ноги")

        val exercise = repository.observeExercises().first { list ->
            list.any { it.name == "Румынская тяга" }
        }.first { it.name == "Румынская тяга" }
        val template = repository.observeTemplates().first { list ->
            list.any { it.name == "Ноги" }
        }.first { it.name == "Ноги" }

        repository.addExerciseToTemplate(template.id, exercise.id)
        val templateExercise = repository.observeTemplates().first { list ->
            list.firstOrNull { it.id == template.id }?.exercises?.size == 1
        }.first { it.id == template.id }.exercises.single()
        repository.updateTemplateExercise(templateExercise.id, 4, 6, 10)

        val configured = repository.observeTemplates().first { list ->
            list.firstOrNull { it.id == template.id }
                ?.exercises
                ?.singleOrNull()
                ?.workSetCount == 4
        }.first { it.id == template.id }
        assertEquals(6, configured.exercises.single().repMin)
        assertEquals(10, configured.exercises.single().repMax)

        repository.startWorkoutFromTemplate(template.id)
        val workout = repository.observeActiveWorkout().filterNotNull().first()
        assertEquals("Румынская тяга", workout.exercises.single().name)
        assertEquals(4, workout.exercises.single().sets.size)
    }

    @Test
    fun activeWorkoutExercisesAndSetsCanBeEdited() = runBlocking {
        repository.ensureStarterData()
        val definitions = repository.observeExercises().first { it.size >= 4 }
        val press = definitions.first { it.name == "Жим гантелей на наклонной" }
        val squat = definitions.first { it.name == "Приседания со штангой" }
        val pulldown = definitions.first { it.name == "Тяга верхнего блока" }
        val sessionId = repository.startEmptyWorkout("Редактируемая")

        repository.addExerciseToActiveWorkout(sessionId, press.id)
        repository.addExerciseToActiveWorkout(sessionId, squat.id)
        val added = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.size == 2 }
        assertEquals(listOf(press.id, squat.id), added.exercises.map { it.definitionId })
        assertEquals(3, added.exercises.first().sets.size)

        repository.moveActiveExercise(added.exercises.last().id, -1)
        val moved = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.firstOrNull()?.definitionId == squat.id }
        val firstExerciseId = moved.exercises.first().id

        repository.replaceActiveExercise(firstExerciseId, pulldown.id)
        val replaced = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.firstOrNull()?.definitionId == pulldown.id }
        assertEquals(3, replaced.exercises.first().sets.size)

        repository.addSetToActiveExercise(firstExerciseId)
        val withExtraSet = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.firstOrNull()?.sets?.size == 4 }
        assertEquals(listOf(0, 1, 2, 3), withExtraSet.exercises.first().sets.map { it.order })

        repository.removeLastSetFromActiveExercise(firstExerciseId)
        val withoutExtraSet = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.firstOrNull()?.sets?.size == 3 }
        assertEquals(3, withoutExtraSet.exercises.first().sets.size)

        repository.removeExerciseFromActiveWorkout(moved.exercises.last().id)
        val afterRemoval = repository.observeActiveWorkout()
            .filterNotNull()
            .first { it.exercises.size == 1 }
        assertEquals(pulldown.id, afterRemoval.exercises.single().definitionId)
    }

    @Test
    fun bodyWeightMeasurementsCanBeSavedAndDeleted() = runBlocking {
        repository.saveBodyWeight(80_000)
        repository.saveBodyWeight(79_500)
        repository.saveBodyWeight(5_000)

        val saved = repository.observeBodyWeight().first { it.size == 2 }
        assertEquals(setOf(80_000L, 79_500L), saved.map { it.weightGrams }.toSet())

        repository.deleteBodyWeight(saved.first().id)
        val afterDeletion = repository.observeBodyWeight().first { it.size == 1 }
        assertEquals(saved.last().weightGrams, afterDeletion.single().weightGrams)
    }

    @Test
    fun backupRestoresDatabaseAndSettings() = runBlocking<Unit> {
        val settings = UserSettingsRepository(context)
        val backup = BackupRepository(context, database, settings)
        repository.ensureStarterData()
        repository.saveBodyWeight(82_400)
        settings.setWeightUnit(WeightUnit.POUNDS)
        settings.setRestSeconds(120)
        val file = File(context.cacheDir, "backup-test.json")

        backup.exportTo(Uri.fromFile(file))
        repository.observeBodyWeight().first { it.isNotEmpty() }.forEach { repository.deleteBodyWeight(it.id) }
        settings.setWeightUnit(WeightUnit.KILOGRAMS)
        backup.importFrom(Uri.fromFile(file))

        assertEquals(82_400L, repository.observeBodyWeight().first { it.isNotEmpty() }.single().weightGrams)
        assertEquals(WeightUnit.POUNDS, settings.settings.value.weightUnit)
        assertEquals(120, settings.settings.value.restSeconds)
        assertEquals(1, repository.observeTemplates().first { it.isNotEmpty() }.size)
        file.delete()
    }
}
