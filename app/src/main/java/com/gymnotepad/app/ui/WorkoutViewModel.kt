package com.gymnotepad.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymnotepad.app.data.ActiveWorkout
import com.gymnotepad.app.data.SetInput
import com.gymnotepad.app.data.ExerciseDefinition
import com.gymnotepad.app.data.WorkoutTemplate
import com.gymnotepad.app.data.WorkoutRepository
import com.gymnotepad.app.data.CompletedWorkout
import com.gymnotepad.app.data.ExerciseProgress
import com.gymnotepad.app.data.WorkoutAnalytics
import com.gymnotepad.app.data.ExerciseRecommendation
import com.gymnotepad.app.data.BodyWeightMeasurement
import com.gymnotepad.app.data.WorkoutRecommendationEngine
import com.gymnotepad.app.domain.EffortMark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.LoadProgressionType

class WorkoutViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val analytics = WorkoutAnalytics()
    private val recommendationEngine = WorkoutRecommendationEngine()
    val lastCompletedSessionId = MutableStateFlow<String?>(null)

    val activeWorkout: StateFlow<ActiveWorkout?> = repository.observeActiveWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val templates: StateFlow<List<WorkoutTemplate>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exercises: StateFlow<List<ExerciseDefinition>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<CompletedWorkout>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bodyWeight: StateFlow<List<BodyWeightMeasurement>> = repository.observeBodyWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<List<ExerciseProgress>> = repository.observeHistory()
        .map(analytics::calculate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recommendations: StateFlow<List<ExerciseRecommendation>> = combine(
        repository.observeHistory(),
        repository.observeExercises(),
        recommendationEngine::calculate,
    )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureStarterData() }
    }

    fun startTemplate(templateId: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            repository.startWorkoutFromTemplate(templateId)
            onStarted()
        }
    }

    fun createTemplate(name: String) {
        viewModelScope.launch { repository.createTemplate(name) }
    }

    fun createExercise(
        name: String,
        loadMode: LoadMode,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    ) {
        viewModelScope.launch {
            repository.createExercise(name, loadMode, progressionType, availableLoadsGrams, barWeightGrams)
        }
    }

    fun addExerciseToTemplate(templateId: String, exerciseId: String) {
        viewModelScope.launch { repository.addExerciseToTemplate(templateId, exerciseId) }
    }

    fun updateExerciseLoadConfiguration(
        exerciseId: String,
        progressionType: LoadProgressionType,
        availableLoadsGrams: List<Long>,
        barWeightGrams: Long?,
    ) {
        viewModelScope.launch {
            repository.updateExerciseLoadConfiguration(
                exerciseId,
                progressionType,
                availableLoadsGrams,
                barWeightGrams,
            )
        }
    }

    fun updateTemplateExercise(
        templateExerciseId: String,
        workSetCount: Int,
        repMin: Int,
        repMax: Int,
    ) {
        viewModelScope.launch {
            repository.updateTemplateExercise(templateExerciseId, workSetCount, repMin, repMax)
        }
    }

    fun startPrevious(onStarted: () -> Unit) {
        viewModelScope.launch {
            repository.startWorkoutFromPrevious("latest")
            onStarted()
        }
    }

    fun startEmpty(onStarted: () -> Unit) {
        viewModelScope.launch {
            repository.startEmptyWorkout("Новая тренировка")
            onStarted()
        }
    }

    fun saveSet(
        setId: String,
        loadGrams: Long?,
        reps: Int?,
        effort: EffortMark,
    ) {
        viewModelScope.launch {
            repository.saveSet(SetInput(setId, loadGrams, reps, effort))
        }
    }

    fun addExerciseToActiveWorkout(sessionId: String, exerciseId: String) {
        viewModelScope.launch { repository.addExerciseToActiveWorkout(sessionId, exerciseId) }
    }

    fun replaceActiveExercise(sessionExerciseId: String, exerciseId: String) {
        viewModelScope.launch { repository.replaceActiveExercise(sessionExerciseId, exerciseId) }
    }

    fun removeExerciseFromActiveWorkout(sessionExerciseId: String) {
        viewModelScope.launch { repository.removeExerciseFromActiveWorkout(sessionExerciseId) }
    }

    fun moveActiveExercise(sessionExerciseId: String, direction: Int) {
        viewModelScope.launch { repository.moveActiveExercise(sessionExerciseId, direction) }
    }

    fun addSetToActiveExercise(sessionExerciseId: String) {
        viewModelScope.launch { repository.addSetToActiveExercise(sessionExerciseId) }
    }

    fun removeLastSetFromActiveExercise(sessionExerciseId: String) {
        viewModelScope.launch { repository.removeLastSetFromActiveExercise(sessionExerciseId) }
    }

    fun completeWorkout(sessionId: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            repository.completeWorkout(sessionId)
            lastCompletedSessionId.value = sessionId
            onCompleted()
        }
    }

    fun saveBodyWeight(weightGrams: Long) {
        viewModelScope.launch { repository.saveBodyWeight(weightGrams) }
    }

    fun deleteBodyWeight(measurementId: String) {
        viewModelScope.launch { repository.deleteBodyWeight(measurementId) }
    }

    class Factory(
        private val repository: WorkoutRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WorkoutViewModel::class.java))
            return WorkoutViewModel(repository) as T
        }
    }
}
