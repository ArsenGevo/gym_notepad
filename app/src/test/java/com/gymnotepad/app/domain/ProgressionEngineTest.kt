package com.gymnotepad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionEngineTest {
    private val engine = ProgressionEngine()

    @Test
    fun `two successful sessions increase external load`() {
        val success = performance(reps = listOf(12, 12, 12), loadGrams = 32_000)

        val result = engine.recommend(
            historyNewestFirst = listOf(success, success),
            loadMode = LoadMode.DUMBBELL_PER_HAND,
            incrementGrams = 2_000,
        )

        assertEquals(RecommendationAction.INCREASE_LOAD, result.action)
        assertEquals(34_000L, result.suggestedLoadGrams)
        assertEquals(2, result.successStreak)
    }

    @Test
    fun `one success keeps load for confirmation`() {
        val result = engine.recommend(
            historyNewestFirst = listOf(performance(listOf(12, 12, 12))),
            loadMode = LoadMode.EXTERNAL_TOTAL,
            incrementGrams = 2_500,
        )

        assertEquals(RecommendationAction.KEEP_LOAD_REPEAT, result.action)
        assertEquals(1, result.successStreak)
    }

    @Test
    fun `failure on last set is borderline`() {
        val result = engine.evaluate(
            performance(
                reps = listOf(12, 12, 12),
                efforts = listOf(EffortMark.RIR_1, EffortMark.RIR_1, EffortMark.FAILURE),
            ),
        )

        assertEquals(WorkoutOutcome.BORDERLINE, result)
    }

    @Test
    fun `pain blocks load increase`() {
        val blocked = performance(listOf(12, 12, 12)).copy(hasPain = true)

        val result = engine.recommend(
            historyNewestFirst = listOf(blocked),
            loadMode = LoadMode.EXTERNAL_TOTAL,
            incrementGrams = 2_500,
        )

        assertEquals(RecommendationAction.MANUAL_REVIEW, result.action)
        assertEquals(WorkoutOutcome.BLOCKED, result.outcome)
    }

    @Test
    fun `assistance progression reduces assistance`() {
        val success = performance(listOf(12, 12, 12), loadGrams = 20_000)

        val result = engine.recommend(
            historyNewestFirst = listOf(success, success),
            loadMode = LoadMode.ASSISTANCE,
            incrementGrams = 5_000,
        )

        assertEquals(15_000L, result.suggestedLoadGrams)
    }

    @Test
    fun `two missed sessions suggest reduction without applying it`() {
        val missed = performance(listOf(7, 7, 6))

        val result = engine.recommend(
            historyNewestFirst = listOf(missed, missed),
            loadMode = LoadMode.EXTERNAL_TOTAL,
            incrementGrams = 2_500,
        )

        assertEquals(RecommendationAction.CONSIDER_REDUCTION, result.action)
        assertEquals(32_000L, result.suggestedLoadGrams)
    }

    private fun performance(
        reps: List<Int>,
        loadGrams: Long = 32_000,
        efforts: List<EffortMark> = List(reps.size) { EffortMark.RIR_1 },
    ) = ExercisePerformance(
        loadGrams = loadGrams,
        plannedWorkSets = 3,
        repMin = 8,
        repMax = 12,
        sets = reps.mapIndexed { index, value ->
            WorkSetPerformance(reps = value, effort = efforts[index])
        },
    )
}

