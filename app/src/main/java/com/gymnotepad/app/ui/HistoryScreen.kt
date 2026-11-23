package com.gymnotepad.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymnotepad.app.data.CompletedWorkout
import com.gymnotepad.app.data.BodyWeightMeasurement
import com.gymnotepad.app.data.ExerciseProgress
import com.gymnotepad.app.data.WorkoutSet
import com.gymnotepad.app.domain.LoadMode
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<CompletedWorkout>,
    progress: List<ExerciseProgress>,
    bodyWeight: List<BodyWeightMeasurement>,
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
                Text("Вес тела", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (bodyWeight.isEmpty()) {
                item { Text("Добавьте измерения на главной, чтобы увидеть динамику.") }
            } else {
                item { BodyWeightProgressCard(bodyWeight) }
            }

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
private fun BodyWeightProgressCard(measurementsNewestFirst: List<BodyWeightMeasurement>) {
    val latest = measurementsNewestFirst.first()
    val previous = measurementsNewestFirst.getOrNull(1)
    val points = measurementsNewestFirst.take(12).asReversed()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Сейчас: ${latest.weightGrams.formatKg()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            previous?.let {
                val delta = latest.weightGrams - it.weightGrams
                Text(
                    when {
                        delta > 0 -> "К предыдущему: +${delta.formatKg()}"
                        delta < 0 -> "К предыдущему: −${(-delta).formatKg()}"
                        else -> "Без изменений относительно предыдущего"
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            TrendChart(
                values = points.map { it.weightGrams / 1_000f },
                timestamps = points.map { it.measuredAt },
                valueFormatter = { value -> "${value.formatChartNumber()} кг" },
            )
        }
    }
}

@Composable
private fun ProgressCard(progress: ExerciseProgress) {
    val canShowLoad = progress.trend.any { it.loadGrams != null }
    var metric by rememberSaveable(progress.exerciseKey) {
        mutableStateOf(if (canShowLoad) ChartMetric.Load else ChartMetric.Repetitions)
    }
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
            if (progress.trend.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                if (canShowLoad) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = metric == ChartMetric.Load,
                            onClick = { metric = ChartMetric.Load },
                            label = { Text(if (progress.loadMode == LoadMode.ASSISTANCE) "Помощь" else "Вес") },
                        )
                        FilterChip(
                            selected = metric == ChartMetric.Repetitions,
                            onClick = { metric = ChartMetric.Repetitions },
                            label = { Text("Повторения") },
                        )
                    }
                }
                val recent = progress.trend.takeLast(8)
                if (metric == ChartMetric.Load && canShowLoad) {
                    if (progress.loadMode == LoadMode.ASSISTANCE) {
                        Text("Динамика помощи — меньше означает прогресс")
                    } else {
                        Text("Динамика рабочего веса")
                    }
                    val loadPoints = recent.filter { it.loadGrams != null }
                    TrendChart(
                        values = loadPoints.map { requireNotNull(it.loadGrams) / 1_000f },
                        timestamps = loadPoints.map { it.completedAt },
                        valueFormatter = { value -> "${value.formatChartNumber()} кг" },
                    )
                } else {
                    Text("Сумма повторений на рабочей нагрузке")
                    TrendChart(
                        values = recent.map { it.repsAtLoad.toFloat() },
                        timestamps = recent.map { it.completedAt },
                        valueFormatter = { value -> "${value.roundToInt()} повт." },
                    )
                }
            }
        }
    }
}

private enum class ChartMetric { Load, Repetitions }

@Composable
private fun TrendChart(
    values: List<Float>,
    timestamps: List<Long>,
    valueFormatter: (Float) -> String,
) {
    if (values.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: min

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(valueFormatter(min), style = MaterialTheme.typography.labelMedium)
        if (max != min) Text(valueFormatter(max), style = MaterialTheme.typography.labelMedium)
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp),
    ) {
        val horizontalPadding = 10.dp.toPx()
        val verticalPadding = 8.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        repeat(3) { index ->
            val y = verticalPadding + chartHeight * index / 2f
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(horizontalPadding, y), end = androidx.compose.ui.geometry.Offset(size.width - horizontalPadding, y))
        }
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) {
                horizontalPadding + chartWidth / 2f
            } else {
                horizontalPadding + chartWidth * index / values.lastIndex
            }
            val y = if (max == min) {
                verticalPadding + chartHeight / 2f
            } else {
                verticalPadding + chartHeight * (1f - (value - min) / (max - min))
            }
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
        points.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
    }
    if (timestamps.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timestamps.first().formatDate(), style = MaterialTheme.typography.labelSmall)
            if (timestamps.size > 1) Text(timestamps.last().formatDate(), style = MaterialTheme.typography.labelSmall)
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

private fun Float.formatChartNumber(): String = BigDecimal.valueOf(toDouble())
    .setScale(1, java.math.RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

private fun Int.signed(): String = if (this > 0) "+$this" else toString()

private fun Long.formatDate(): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(this))
