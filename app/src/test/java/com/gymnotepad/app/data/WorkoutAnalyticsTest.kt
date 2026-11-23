package com.gymnotepad.app.data

import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutAnalyticsTest {
    private val analytics = WorkoutAnalytics()

    @Test
    fun `counts rep growth at the same working load`() {
        val result = analytics.calculate(
            listOf(
                workout(exercise(LoadMode.DUMBBELL_PER_HAND, 32_000, listOf(10, 10, 9))),
                workout(exercise(LoadMode.DUMBBELL_PER_HAND, 32_000, listOf(9, 9, 8))),
            ),
        ).single()

        assertEquals(32_000L, result.currentLoadGrams)
        assertEquals(29, result.currentRepsAtLoad)
        assertEquals(26, result.previousRepsAtSameLoad)
        assertEquals(3, result.repsChangeAtSameLoad)
        assertEquals(0L, result.difficultyChangeGrams)
    }

    @Test
    fun `reports load increase separately from same-load reps`() {
        val result = analytics.calculate(
            listOf(
                workout(exercise(LoadMode.EXTERNAL_TOTAL, 34_000, listOf(8, 8, 8))),
                workout(exercise(LoadMode.EXTERNAL_TOTAL, 32_000, listOf(12, 12, 12))),
            ),
        ).single()

        assertEquals(2_000L, result.difficultyChangeGrams)
        assertNull(result.previousRepsAtSameLoad)
        assertNull(result.repsChangeAtSameLoad)
    }

    @Test
    fun `less assistance is positive difficulty progress`() {
        val result = analytics.calculate(
            listOf(
                workout(exercise(LoadMode.ASSISTANCE, 15_000, listOf(8, 8, 8))),
                workout(exercise(LoadMode.ASSISTANCE, 20_000, listOf(8, 8, 8))),
            ),
        ).single()

        assertEquals(5_000L, result.difficultyChangeGrams)
    }

    private fun workout(exercise: WorkoutExercise) = CompletedWorkout(
        id = "workout-${exercise.sets.first().loadGrams}",
        name = "Тренировка",
        startedAt = 0,
        completedAt = 1,
        exercises = listOf(exercise),
    )

    private fun exercise(mode: LoadMode, load: Long, reps: List<Int>) = WorkoutExercise(
        id = "session-exercise-$load",
        definitionId = "exercise-1",
        name = "Упражнение",
        loadMode = mode,
        repMin = 8,
        repMax = 12,
        sets = reps.mapIndexed { index, value ->
            WorkoutSet(
                id = "set-$load-$index",
                order = index,
                loadGrams = load,
                reps = value,
                effort = EffortMark.RIR_1,
                completed = true,
            )
        },
    )
}
