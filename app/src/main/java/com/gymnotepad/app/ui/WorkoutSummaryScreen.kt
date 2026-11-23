package com.gymnotepad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.gymnotepad.app.data.ExerciseRecommendation
import com.gymnotepad.app.domain.RecommendationAction
import com.gymnotepad.app.domain.WorkoutOutcome
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    workout: CompletedWorkout?,
    recommendations: List<ExerciseRecommendation>,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Итоги тренировки") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (workout == null) {
                item { Text("Сохраняем результаты и формируем рекомендации…") }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(workout.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Длительность: ${formatDuration(workout.completedAt - workout.startedAt)}")
                            Text("Упражнений: ${workout.exercises.size}")
                            Text("Выполнено подходов: ${workout.exercises.sumOf { exercise -> exercise.sets.count { it.completed } }}")
                        }
                    }
                }

                item {
                    Text(
                        "Рекомендации на следующий раз",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (recommendations.isEmpty()) {
                    item { Text("Недостаточно данных для рекомендаций") }
                } else {
                    items(recommendations, key = { it.exerciseKey }) { item ->
                        RecommendationCard(item)
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDone,
                ) { Text("На главную") }
            }
        }
    }
}

@Composable
private fun RecommendationCard(item: ExerciseRecommendation) {
    val recommendation = item.recommendation
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(item.exerciseName, fontWeight = FontWeight.Bold)
            Text(
                recommendation.action.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Результат: ${recommendation.outcome.displayName()}")
            Text(recommendation.reason)
            recommendation.suggestedLoadGrams?.let { suggested ->
                if (recommendation.action == RecommendationAction.INCREASE_LOAD) {
                    Text("Следующая нагрузка: ${suggested.formatKg()}", fontWeight = FontWeight.Bold)
                }
            }
            if (recommendation.action == RecommendationAction.INCREASE_LOAD && item.platesPerSideGrams.isNotEmpty()) {
                Text("На одну сторону: ${item.platesPerSideGrams.groupingBy { it }.eachCount().entries.joinToString(" + ") { (plate, count) -> "$count × ${plate.formatKg()}" }}")
            }
            if (recommendation.successStreak > 0) {
                Text("Успешных тренировок подряд: ${recommendation.successStreak}")
            }
        }
    }
}

private fun RecommendationAction.displayName(): String = when (this) {
    RecommendationAction.INCREASE_LOAD -> "Увеличить нагрузку"
    RecommendationAction.KEEP_LOAD_ADD_REPS -> "Сохранить вес и добавить повторения"
    RecommendationAction.KEEP_LOAD_REPEAT -> "Повторить текущую нагрузку"
    RecommendationAction.CONSIDER_REDUCTION -> "Рассмотреть снижение нагрузки"
    RecommendationAction.MANUAL_REVIEW -> "Выбрать следующий доступный вес"
    RecommendationAction.NONE -> "Продолжить собирать данные"
}

private fun WorkoutOutcome.displayName(): String = when (this) {
    WorkoutOutcome.SUCCESS -> "цель выполнена"
    WorkoutOutcome.BORDERLINE -> "цель выполнена на пределе"
    WorkoutOutcome.PROGRESSING -> "есть прогресс"
    WorkoutOutcome.MISSED -> "цель не достигнута"
    WorkoutOutcome.BLOCKED -> "нужна ручная проверка"
    WorkoutOutcome.INSUFFICIENT_DATA -> "недостаточно данных"
}

private fun formatDuration(milliseconds: Long): String {
    val totalMinutes = (milliseconds.coerceAtLeast(0) / 60_000)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours ч $minutes мин" else "$minutes мин"
}

private fun Long.formatKg(): String = BigDecimal.valueOf(this, 3).stripTrailingZeros().toPlainString() + " кг"
