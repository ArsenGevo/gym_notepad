package com.gymnotepad.app.data

import com.gymnotepad.app.domain.LoadMode

data class ExerciseProgress(
    val exerciseKey: String,
    val exerciseName: String,
    val loadMode: LoadMode,
    val currentLoadGrams: Long?,
    val difficultyChangeGrams: Long?,
    val currentRepsAtLoad: Int,
    val previousRepsAtSameLoad: Int?,
    val repsChangeAtSameLoad: Int?,
)

class WorkoutAnalytics {
    fun calculate(historyNewestFirst: List<CompletedWorkout>): List<ExerciseProgress> {
        val occurrences = historyNewestFirst.flatMap { workout ->
            workout.exercises.map { exercise ->
                Occurrence(
                    key = exercise.definitionId ?: "${exercise.name}|${exercise.loadMode}",
                    exercise = exercise,
                )
            }
        }.groupBy(Occurrence::key)

        return occurrences.values.mapNotNull { exerciseHistory ->
            val current = exerciseHistory.firstOrNull()?.exercise ?: return@mapNotNull null
            val currentLoad = current.representativeLoad()
            val previous = exerciseHistory.drop(1).firstOrNull()?.exercise
            val previousLoad = previous?.representativeLoad()
            val sameLoadPrevious = exerciseHistory.drop(1)
                .map(Occurrence::exercise)
                .firstOrNull { it.representativeLoad() == currentLoad }
            val currentReps = current.repsAt(currentLoad)
            val previousSameReps = sameLoadPrevious?.repsAt(currentLoad)

            ExerciseProgress(
                exerciseKey = exerciseHistory.first().key,
                exerciseName = current.name,
                loadMode = current.loadMode,
                currentLoadGrams = currentLoad,
                difficultyChangeGrams = difficultyDelta(current.loadMode, currentLoad, previousLoad),
                currentRepsAtLoad = currentReps,
                previousRepsAtSameLoad = previousSameReps,
                repsChangeAtSameLoad = previousSameReps?.let { currentReps - it },
            )
        }.sortedBy(ExerciseProgress::exerciseName)
    }
}

private data class Occurrence(
    val key: String,
    val exercise: WorkoutExercise,
)

private fun WorkoutExercise.representativeLoad(): Long? {
    val loads = sets.filter(WorkoutSet::completed).mapNotNull(WorkoutSet::loadGrams)
    return when (loadMode) {
        LoadMode.BODYWEIGHT -> null
        LoadMode.ASSISTANCE -> loads.minOrNull()
        else -> loads.maxOrNull()
    }
}

private fun WorkoutExercise.repsAt(loadGrams: Long?): Int = sets
    .filter { it.completed && it.loadGrams == loadGrams }
    .sumOf { it.reps ?: 0 }

private fun difficultyDelta(mode: LoadMode, current: Long?, previous: Long?): Long? {
    if (current == null || previous == null) return null
    return if (mode == LoadMode.ASSISTANCE) previous - current else current - previous
}
