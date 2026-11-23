package com.gymnotepad.app.data

import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.LoadProgressionType
import com.gymnotepad.app.domain.RecommendationAction
import com.gymnotepad.app.domain.WorkoutOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutRecommendationEngineTest {
    private val engine = WorkoutRecommendationEngine()

    @Test
    fun `rep improvement at same load recommends adding reps`() {
        val result = engine.calculate(
            listOf(
                workout(32_000, listOf(10, 10, 9)),
                workout(32_000, listOf(9, 9, 8)),
            ),
            definitions(),
        ).single().recommendation

        assertEquals(WorkoutOutcome.PROGRESSING, result.outcome)
        assertEquals(RecommendationAction.KEEP_LOAD_ADD_REPS, result.action)
    }

    @Test
    fun `two successful dumbbell sessions recommend two kilogram increase`() {
        val result = engine.calculate(
            listOf(
                workout(32_000, listOf(12, 12, 12)),
                workout(32_000, listOf(12, 12, 12)),
            ),
            definitions(),
        ).single().recommendation

        assertEquals(WorkoutOutcome.SUCCESS, result.outcome)
        assertEquals(2, result.successStreak)
        assertEquals(RecommendationAction.INCREASE_LOAD, result.action)
        assertEquals(34_000L, result.suggestedLoadGrams)
    }

    @Test
    fun `two missed sessions recommend considering reduction`() {
        val result = engine.calculate(
            listOf(
                workout(32_000, listOf(7, 7, 6)),
                workout(32_000, listOf(7, 7, 7)),
            ),
            definitions(),
        ).single().recommendation

        assertEquals(WorkoutOutcome.MISSED, result.outcome)
        assertEquals(RecommendationAction.CONSIDER_REDUCTION, result.action)
    }

    private fun workout(load: Long, reps: List<Int>) = CompletedWorkout(
        id = "workout-${reps.joinToString()}",
        name = "Тренировка",
        startedAt = 0,
        completedAt = 1,
        exercises = listOf(
            WorkoutExercise(
                id = "session-exercise",
                definitionId = "exercise-1",
                name = "Жим",
                loadMode = LoadMode.DUMBBELL_PER_HAND,
                repMin = 8,
                repMax = 12,
                sets = reps.mapIndexed { index, value ->
                    WorkoutSet(
                        id = "set-$index",
                        order = index,
                        loadGrams = load,
                        reps = value,
                        effort = EffortMark.RIR_1,
                        completed = true,
                    )
                },
            ),
        ),
    )

    private fun definitions() = listOf(
        ExerciseDefinition(
            id = "exercise-1",
            name = "Жим",
            loadMode = LoadMode.DUMBBELL_PER_HAND,
            progressionType = LoadProgressionType.DUMBBELL_2_KG,
            availableLoadsGrams = emptyList(),
            barWeightGrams = null,
        ),
    )
}
