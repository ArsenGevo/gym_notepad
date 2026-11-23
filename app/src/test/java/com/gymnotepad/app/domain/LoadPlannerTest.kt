package com.gymnotepad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadPlannerTest {
    private val planner = LoadPlanner()

    @Test
    fun `dumbbells increase by two kilograms per dumbbell`() {
        val result = planner.nextLoad(
            LoadProgressionType.DUMBBELL_2_KG,
            LoadMode.DUMBBELL_PER_HAND,
            32_000,
            emptyList(),
            null,
        )

        assertEquals(34_000L, result?.nextLoadGrams)
    }

    @Test
    fun `barbell adds smallest plates symmetrically and calculates full loading`() {
        val result = planner.nextLoad(
            LoadProgressionType.BARBELL_PLATES,
            LoadMode.EXTERNAL_TOTAL,
            80_000,
            emptyList(),
            20_000,
        )

        assertEquals(82_500L, result?.nextLoadGrams)
        assertEquals(listOf(25_000L, 5_000L, 1_250L), result?.platesPerSideGrams)
    }

    @Test
    fun `machine uses its actual next stack value`() {
        val result = planner.nextLoad(
            LoadProgressionType.CUSTOM_VALUES,
            LoadMode.MACHINE,
            15_000,
            listOf(5_000, 10_000, 15_000, 22_500, 30_000),
            null,
        )

        assertEquals(22_500L, result?.nextLoadGrams)
    }

    @Test
    fun `assistance moves to next lower available value`() {
        val result = planner.nextLoad(
            LoadProgressionType.CUSTOM_VALUES,
            LoadMode.ASSISTANCE,
            20_000,
            listOf(10_000, 15_000, 20_000, 25_000),
            null,
        )

        assertEquals(15_000L, result?.nextLoadGrams)
    }

    @Test
    fun `custom machine without a next value does not invent one`() {
        val result = planner.nextLoad(
            LoadProgressionType.CUSTOM_VALUES,
            LoadMode.MACHINE,
            30_000,
            listOf(10_000, 20_000, 30_000),
            null,
        )

        assertNull(result)
    }
}
