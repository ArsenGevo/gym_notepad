package com.gymnotepad.app.domain

enum class LoadMode {
    EXTERNAL_TOTAL,
    DUMBBELL_PER_HAND,
    MACHINE,
    BODYWEIGHT,
    BODYWEIGHT_ADDED,
    ASSISTANCE,
}

enum class LoadProgressionType {
    DUMBBELL_2_KG,
    CUSTOM_VALUES,
    BARBELL_PLATES,
    PLATE_LOADED,
    NONE,
}

enum class EffortMark {
    UNKNOWN,
    RIR_2_PLUS,
    RIR_1,
    FAILURE,
}

enum class WorkoutOutcome {
    SUCCESS,
    BORDERLINE,
    PROGRESSING,
    MISSED,
    BLOCKED,
    INSUFFICIENT_DATA,
}

enum class RecommendationAction {
    INCREASE_LOAD,
    KEEP_LOAD_ADD_REPS,
    KEEP_LOAD_REPEAT,
    CONSIDER_REDUCTION,
    MANUAL_REVIEW,
    NONE,
}

data class WorkSetPerformance(
    val reps: Int?,
    val effort: EffortMark = EffortMark.UNKNOWN,
)

data class ExercisePerformance(
    val loadGrams: Long?,
    val plannedWorkSets: Int,
    val repMin: Int,
    val repMax: Int,
    val sets: List<WorkSetPerformance>,
    val hasPain: Boolean = false,
    val hadTechniqueBreakdown: Boolean = false,
    val improvedAtSameLoad: Boolean = false,
)

data class Recommendation(
    val outcome: WorkoutOutcome,
    val action: RecommendationAction,
    val suggestedLoadGrams: Long?,
    val successStreak: Int,
    val reason: String,
)

class ProgressionEngine {
    fun evaluate(performance: ExercisePerformance): WorkoutOutcome {
        if (performance.hasPain || performance.hadTechniqueBreakdown) {
            return WorkoutOutcome.BLOCKED
        }

        if (performance.sets.size < performance.plannedWorkSets) {
            return WorkoutOutcome.MISSED
        }

        val plannedSets = performance.sets.take(performance.plannedWorkSets)
        if (plannedSets.any { it.reps == null }) {
            return WorkoutOutcome.MISSED
        }

        if (plannedSets.dropLast(1).any { it.effort == EffortMark.FAILURE }) {
            return WorkoutOutcome.MISSED
        }

        if (plannedSets.all { requireNotNull(it.reps) >= performance.repMax }) {
            return if (plannedSets.last().effort == EffortMark.FAILURE) {
                WorkoutOutcome.BORDERLINE
            } else {
                WorkoutOutcome.SUCCESS
            }
        }

        if (plannedSets.any { requireNotNull(it.reps) < performance.repMin }) {
            return WorkoutOutcome.MISSED
        }

        return if (performance.improvedAtSameLoad) {
            WorkoutOutcome.PROGRESSING
        } else {
            WorkoutOutcome.BORDERLINE
        }
    }

    fun recommend(
        historyNewestFirst: List<ExercisePerformance>,
        loadMode: LoadMode,
        incrementGrams: Long?,
        successesRequired: Int = 2,
    ): Recommendation {
        if (historyNewestFirst.isEmpty()) {
            return Recommendation(
                outcome = WorkoutOutcome.INSUFFICIENT_DATA,
                action = RecommendationAction.NONE,
                suggestedLoadGrams = null,
                successStreak = 0,
                reason = "Недостаточно истории для рекомендации",
            )
        }

        val outcomes = historyNewestFirst.map(::evaluate)
        val current = outcomes.first()
        val successStreak = outcomes.takeWhile { it == WorkoutOutcome.SUCCESS }.size
        val currentLoad = historyNewestFirst.first().loadGrams

        if (current == WorkoutOutcome.BLOCKED) {
            return Recommendation(
                outcome = current,
                action = RecommendationAction.MANUAL_REVIEW,
                suggestedLoadGrams = currentLoad,
                successStreak = 0,
                reason = "Увеличение нагрузки заблокировано отметкой боли или техники",
            )
        }

        if (successStreak >= successesRequired) {
            if (loadMode == LoadMode.BODYWEIGHT || currentLoad == null || incrementGrams == null) {
                return Recommendation(
                    outcome = current,
                    action = RecommendationAction.MANUAL_REVIEW,
                    suggestedLoadGrams = currentLoad,
                    successStreak = successStreak,
                    reason = "Цель достигнута; выберите следующий доступный вариант нагрузки",
                )
            }

            val nextLoad = when (loadMode) {
                LoadMode.ASSISTANCE -> (currentLoad - incrementGrams).coerceAtLeast(0)
                else -> currentLoad + incrementGrams
            }
            return Recommendation(
                outcome = current,
                action = RecommendationAction.INCREASE_LOAD,
                suggestedLoadGrams = nextLoad,
                successStreak = successStreak,
                reason = "Цель выполнена в $successStreak тренировках подряд",
            )
        }

        if (current == WorkoutOutcome.PROGRESSING) {
            return Recommendation(
                outcome = current,
                action = RecommendationAction.KEEP_LOAD_ADD_REPS,
                suggestedLoadGrams = currentLoad,
                successStreak = successStreak,
                reason = "Повторения выросли; сохраните вес и продолжайте прогрессию",
            )
        }

        if (outcomes.take(2).size == 2 && outcomes.take(2).all { it == WorkoutOutcome.MISSED }) {
            return Recommendation(
                outcome = current,
                action = RecommendationAction.CONSIDER_REDUCTION,
                suggestedLoadGrams = currentLoad,
                successStreak = 0,
                reason = "Целевой диапазон не достигнут два раза подряд",
            )
        }

        return Recommendation(
            outcome = current,
            action = RecommendationAction.KEEP_LOAD_REPEAT,
            suggestedLoadGrams = currentLoad,
            successStreak = successStreak,
            reason = "Сохраните нагрузку и повторите попытку",
        )
    }
}

