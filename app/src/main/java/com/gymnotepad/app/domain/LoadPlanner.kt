package com.gymnotepad.app.domain

data class LoadPlan(
    val nextLoadGrams: Long,
    val platesPerSideGrams: List<Long> = emptyList(),
)

class LoadPlanner {
    fun nextLoad(
        progressionType: LoadProgressionType,
        loadMode: LoadMode,
        currentLoadGrams: Long?,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    ): LoadPlan? {
        val current = currentLoadGrams ?: return null
        val next = when (progressionType) {
            LoadProgressionType.DUMBBELL_2_KG -> current + 2_000
            LoadProgressionType.CUSTOM_VALUES -> if (loadMode == LoadMode.ASSISTANCE) {
                availableLoadsGrams.filter { it < current }.maxOrNull()
            } else {
                availableLoadsGrams.filter { it > current }.minOrNull()
            }
            LoadProgressionType.BARBELL_PLATES,
            LoadProgressionType.PLATE_LOADED,
            -> current + 2 * STANDARD_PLATES_GRAMS.min()
            LoadProgressionType.NONE -> null
        } ?: return null

        val plates = when (progressionType) {
            LoadProgressionType.BARBELL_PLATES -> platesPerSide(next, barWeightGrams ?: 20_000)
            LoadProgressionType.PLATE_LOADED -> platesPerSide(next, barWeightGrams ?: 0)
            else -> emptyList()
        }
        return LoadPlan(next, plates)
    }

    fun platesPerSide(totalLoadGrams: Long, barWeightGrams: Long): List<Long> {
        val platesTotal = totalLoadGrams - barWeightGrams
        if (platesTotal < 0 || platesTotal % 2 != 0L) return emptyList()
        var remaining = platesTotal / 2
        val result = mutableListOf<Long>()
        STANDARD_PLATES_GRAMS.sortedDescending().forEach { plate ->
            while (remaining >= plate) {
                result += plate
                remaining -= plate
            }
        }
        return if (remaining == 0L) result else emptyList()
    }

    companion object {
        val STANDARD_PLATES_GRAMS = listOf(1_250L, 2_500L, 5_000L, 10_000L, 15_000L, 20_000L, 25_000L)
    }
}
