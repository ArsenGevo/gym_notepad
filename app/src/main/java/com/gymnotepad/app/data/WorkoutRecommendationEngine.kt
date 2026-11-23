package com.gymnotepad.app.data

import com.gymnotepad.app.domain.ExercisePerformance
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.ProgressionEngine
import com.gymnotepad.app.domain.LoadPlanner
import com.gymnotepad.app.domain.Recommendation
import com.gymnotepad.app.domain.WorkSetPerformance

data class ExerciseRecommendation(
    val exerciseKey: String,
    val exerciseName: String,
    val loadMode: LoadMode,
    val recommendation: Recommendation,
    val platesPerSideGrams: List<Long>,
)

class WorkoutRecommendationEngine(
    private val progressionEngine: ProgressionEngine = ProgressionEngine(),
    private val loadPlanner: LoadPlanner = LoadPlanner(),
) {
    fun calculate(
        historyNewestFirst: List<CompletedWorkout>,
        exerciseDefinitions: List<ExerciseDefinition>,
    ): List<ExerciseRecommendation> {
        val latestWorkout = historyNewestFirst.firstOrNull() ?: return emptyList()
        return latestWorkout.exercises.map { latestExercise ->
            val key = latestExercise.key()
            val exerciseHistory = historyNewestFirst
                .flatMap(CompletedWorkout::exercises)
                .filter { it.key() == key }
            val performances = exerciseHistory.mapIndexed { index, exercise ->
                val load = exercise.representativeLoadForRecommendation()
                val olderAtSameLoad = exerciseHistory.drop(index + 1)
                    .firstOrNull { it.representativeLoadForRecommendation() == load }
                val currentReps = exercise.completedRepsAt(load)
                val olderReps = olderAtSameLoad?.completedRepsAt(load)
                ExercisePerformance(
                    loadGrams = load,
                    plannedWorkSets = exercise.sets.size,
                    repMin = exercise.repMin,
                    repMax = exercise.repMax,
                    sets = exercise.sets
                        .filter(WorkoutSet::completed)
                        .sortedBy(WorkoutSet::order)
                        .map { WorkSetPerformance(it.reps, it.effort) },
                    improvedAtSameLoad = olderReps != null && currentReps > olderReps,
                )
            }

            val definition = exerciseDefinitions.firstOrNull { it.id == latestExercise.definitionId }
            val currentLoad = latestExercise.representativeLoadForRecommendation()
            val loadPlan = definition?.let {
                loadPlanner.nextLoad(
                    progressionType = it.progressionType,
                    loadMode = it.loadMode,
                    currentLoadGrams = currentLoad,
                    availableLoadsGrams = it.availableLoadsGrams,
                    barWeightGrams = it.barWeightGrams,
                )
            }
            ExerciseRecommendation(
                exerciseKey = key,
                exerciseName = latestExercise.name,
                loadMode = latestExercise.loadMode,
                recommendation = progressionEngine.recommend(
                    historyNewestFirst = performances,
                    loadMode = latestExercise.loadMode,
                    incrementGrams = loadPlan?.let { plan ->
                        kotlin.math.abs(plan.nextLoadGrams - requireNotNull(currentLoad))
                    },
                ),
                platesPerSideGrams = loadPlan?.platesPerSideGrams.orEmpty(),
            )
        }
    }
}

private fun WorkoutExercise.key(): String = definitionId ?: "$name|$loadMode"

private fun WorkoutExercise.representativeLoadForRecommendation(): Long? {
    val loads = sets.filter(WorkoutSet::completed).mapNotNull(WorkoutSet::loadGrams)
    return when (loadMode) {
        LoadMode.BODYWEIGHT -> null
        LoadMode.ASSISTANCE -> loads.minOrNull()
        else -> loads.maxOrNull()
    }
}

private fun WorkoutExercise.completedRepsAt(load: Long?): Int = sets
    .filter { it.completed && it.loadGrams == load }
    .sumOf { it.reps ?: 0 }
