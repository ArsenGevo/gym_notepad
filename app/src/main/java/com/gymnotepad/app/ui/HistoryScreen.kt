package com.gymnotepad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymnotepad.app.data.CompletedWorkout
import com.gymnotepad.app.data.ExerciseProgress
import com.gymnotepad.app.data.WorkoutSet
import com.gymnotepad.app.domain.LoadMode
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<CompletedWorkout>,
    progress: List<ExerciseProgress>,
    onNavigate: (AppRoute) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("История") }) },
        bottomBar = {
            MainNavigationBar(
                current = AppRoute.History,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Прогресс упражнений", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (progress.isEmpty()) {
                item { Text("Завершите первую тренировку, чтобы появилась аналитика.") }
            } else {
                items(progress, key = { it.exerciseKey }) { item -> ProgressCard(item) }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Тренировки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (history.isEmpty()) {
                item { Text("История пока пуста") }
            } else {
                items(history, key = { it.id }) { workout -> WorkoutHistoryCard(workout) }
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: ExerciseProgress) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(progress.exerciseName, fontWeight = FontWeight.Bold)
            Text("Рабочая нагрузка: ${progress.currentLoadGrams.formatLoad(progress.loadMode)}")
            progress.difficultyChangeGrams?.let { delta ->
                Text(
                    text = when {
                        delta > 0 -> "Сложность выросла на ${delta.formatKg()}"
                        delta < 0 -> "Сложность снизилась на ${(-delta).formatKg()}"
                        else -> "Рабочая нагрузка без изменений"
                    },
                    color = if (delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            val previousReps = progress.previousRepsAtSameLoad
            if (previousReps == null) {
                Text("Повторения при этой нагрузке: ${progress.currentRepsAtLoad}")
            } else {
                val delta = progress.repsChangeAtSameLoad ?: 0
                Text("При том же весе: $previousReps → ${progress.currentRepsAtLoad} (${delta.signed()})")
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(workout: CompletedWorkout) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(workout.name, fontWeight = FontWeight.Bold)
                Text(workout.completedAt.formatDate())
            }
            workout.exercises.forEach { exercise ->
                Spacer(Modifier.height(8.dp))
                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                Text(
                    exercise.sets
                        .filter(WorkoutSet::completed)
                        .joinToString(" · ") { set ->
                            "${set.loadGrams.formatLoad(exercise.loadMode)} × ${set.reps ?: "—"}"
                        }
                        .ifEmpty { "Нет завершённых подходов" },
                )
            }
        }
    }
}

private fun Long?.formatLoad(mode: LoadMode): String = when {
    mode == LoadMode.BODYWEIGHT -> "вес тела"
    this == null -> "—"
    mode == LoadMode.ASSISTANCE -> "${formatKg()} помощи"
    else -> formatKg()
}

private fun Long.formatKg(): String = BigDecimal.valueOf(this, 3)
    .stripTrailingZeros()
    .toPlainString() + " кг"

private fun Int.signed(): String = if (this > 0) "+$this" else toString()

private fun Long.formatDate(): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(this))
