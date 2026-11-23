package com.gymnotepad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymnotepad.app.domain.EffortMark
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.adjustRestDuration
import com.gymnotepad.app.domain.formatRestDuration
import com.gymnotepad.app.domain.remainingRestSeconds
import com.gymnotepad.app.data.ActiveWorkout
import com.gymnotepad.app.data.CompletedWorkout
import com.gymnotepad.app.data.BodyWeightMeasurement
import com.gymnotepad.app.data.ExerciseDefinition
import com.gymnotepad.app.data.WorkoutSet
import com.gymnotepad.app.data.WorkoutTemplate
import com.gymnotepad.app.data.WeightUnit
import com.gymnotepad.app.domain.symbol
import com.gymnotepad.app.domain.toWeightGrams
import com.gymnotepad.app.domain.toWeightText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val mainDestinations = listOf(
    AppRoute.Home,
    AppRoute.History,
    AppRoute.Templates,
    AppRoute.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activeWorkout: ActiveWorkout?,
    lastWorkout: CompletedWorkout?,
    bodyWeight: List<BodyWeightMeasurement>,
    onSaveBodyWeight: (Long) -> Unit,
    onDeleteBodyWeight: (String) -> Unit,
    weightUnit: WeightUnit,
    onStartWorkout: () -> Unit,
    onContinueWorkout: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var weightToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val parsedWeight = weightInput.toWeightGrams(weightUnit)
    val latestWeight = bodyWeight.firstOrNull()

    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Записать вес тела") },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Вес, ${weightUnit.symbol}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Допустимый диапазон: 20–500 кг") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsedWeight in 20_000L..500_000L,
                    onClick = {
                        parsedWeight?.let(onSaveBodyWeight)
                        showWeightDialog = false
                    },
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text("Отмена") }
            },
        )
    }

    weightToDelete?.let { measurementId ->
        AlertDialog(
            onDismissRequest = { weightToDelete = null },
            title = { Text("Удалить измерение?") },
            text = { Text("Последнее измерение веса будет удалено.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteBodyWeight(measurementId)
                    weightToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { weightToDelete = null }) { Text("Отмена") }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gym Notepad") }) },
        bottomBar = {
            MainNavigationBar(
                current = AppRoute.Home,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Готовы к тренировке?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text("Результаты сохраняются на устройстве и доступны без интернета.")
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (activeWorkout == null) onStartWorkout else onContinueWorkout,
                ) {
                    Text(if (activeWorkout == null) "Начать тренировку" else "Продолжить тренировку")
                }
            }
            if (activeWorkout != null) {
                item {
                    val allSets = activeWorkout.exercises.flatMap { it.sets }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(activeWorkout.name, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Сохранено подходов: ${allSets.count { it.completed }} из ${allSets.size}")
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Последняя тренировка", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(lastWorkout?.name ?: "Пока нет завершённых тренировок")
                        if (lastWorkout != null) {
                            Text("Упражнений: ${lastWorkout.exercises.size}")
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Вес тела", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        if (latestWeight == null) {
                            Text("Добавьте первое измерение")
                        } else {
                            Text(
                                "${latestWeight.weightGrams.toWeightText(weightUnit)} ${weightUnit.symbol}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Измерено ${latestWeight.measuredAt.toMeasurementDate()}")
                            bodyWeight.getOrNull(1)?.let { previous ->
                                val delta = latestWeight.weightGrams - previous.weightGrams
                                Text("К предыдущему: ${delta.toSignedWeightText(weightUnit)} ${weightUnit.symbol}")
                            }
                            if (bodyWeight.size > 1) {
                                Spacer(Modifier.height(12.dp))
                                Text("Последние измерения", fontWeight = FontWeight.Bold)
                                bodyWeight.take(5).forEach { measurement ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(measurement.measuredAt.toMeasurementDate())
                                        Text("${measurement.weightGrams.toWeightText(weightUnit)} ${weightUnit.symbol}")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    weightInput = latestWeight?.weightGrams?.toWeightText(weightUnit).orEmpty()
                                    showWeightDialog = true
                                },
                            ) { Text(if (latestWeight == null) "Добавить" else "Новое измерение") }
                            if (latestWeight != null) {
                                OutlinedButton(onClick = { weightToDelete = latestWeight.id }) {
                                    Text("Удалить последнее")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutScreen(
    templates: List<WorkoutTemplate>,
    onBack: () -> Unit,
    onStartTemplate: (String) -> Unit,
    onStartPrevious: () -> Unit,
    onStartEmpty: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Начать тренировку") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Из шаблона", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (templates.isEmpty()) {
                item { Text("Шаблоны загружаются…") }
            } else {
                items(templates.size, key = { templates[it].id }) { index ->
                    val template = templates[index]
                    StartOptionCard(
                        title = template.name,
                        description = "Упражнений: ${template.exercises.size}",
                        onClick = { onStartTemplate(template.id) },
                    )
                }
            }
            item {
                StartOptionCard(
                    title = "Повторить прошлую",
                    description = "Скопировать упражнения и целевые подходы",
                    onClick = onStartPrevious,
                )
            }
            item {
                StartOptionCard(
                    title = "Пустая тренировка",
                    description = "Добавить упражнения непосредственно в зале",
                    onClick = onStartEmpty,
                )
            }
        }
    }
}

@Composable
private fun StartOptionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout?,
    exercises: List<ExerciseDefinition>,
    weightUnit: WeightUnit,
    defaultRestSeconds: Int,
    defaultAutoStartRestTimer: Boolean,
    onSaveSet: (String, Long?, Int?, EffortMark) -> Unit,
    onAddExercise: (String, String) -> Unit,
    onReplaceExercise: (String, String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveLastSet: (String) -> Unit,
    onFinish: (String) -> Unit,
) {
    var showExercisePicker by rememberSaveable { mutableStateOf(false) }
    var exerciseToReplace by rememberSaveable { mutableStateOf<String?>(null) }
    var exerciseToRemove by rememberSaveable { mutableStateOf<String?>(null) }
    var showFinishWarning by rememberSaveable { mutableStateOf(false) }
    var restDurationSeconds by rememberSaveable { mutableIntStateOf(defaultRestSeconds) }
    var restSecondsRemaining by rememberSaveable { mutableIntStateOf(defaultRestSeconds) }
    var restTimerEndMillis by rememberSaveable { mutableLongStateOf(0L) }
    var restTimerRunning by rememberSaveable { mutableStateOf(false) }
    var restTimerAutoStart by rememberSaveable { mutableStateOf(defaultAutoStartRestTimer) }
    val incompleteSetCount = workout?.exercises?.sumOf { exercise ->
        exercise.sets.count { !it.completed }
    } ?: 0
    val startRestTimer: (Int) -> Unit = { seconds ->
        restSecondsRemaining = seconds
        restTimerEndMillis = System.currentTimeMillis() + seconds * 1_000L
        restTimerRunning = true
    }

    LaunchedEffect(restTimerRunning, restTimerEndMillis) {
        while (restTimerRunning) {
            val remaining = remainingRestSeconds(restTimerEndMillis, System.currentTimeMillis())
            restSecondsRemaining = remaining
            if (remaining == 0) {
                restTimerRunning = false
            } else {
                delay(250L)
            }
        }
    }

    if (showExercisePicker || exerciseToReplace != null) {
        AlertDialog(
            onDismissRequest = {
                showExercisePicker = false
                exerciseToReplace = null
            },
            title = { Text(if (exerciseToReplace == null) "Добавить упражнение" else "Заменить упражнение") },
            text = {
                if (exercises.isEmpty()) {
                    Text("Каталог упражнений пуст")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(exercises, key = { it.id }) { exercise ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val replacing = exerciseToReplace
                                    if (replacing == null) {
                                        workout?.let { onAddExercise(it.id, exercise.id) }
                                    } else {
                                        onReplaceExercise(replacing, exercise.id)
                                    }
                                    showExercisePicker = false
                                    exerciseToReplace = null
                                },
                            ) { Text(exercise.name) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExercisePicker = false
                    exerciseToReplace = null
                }) { Text("Отмена") }
            },
        )
    }

    exerciseToRemove?.let { exerciseId ->
        AlertDialog(
            onDismissRequest = { exerciseToRemove = null },
            title = { Text("Удалить упражнение?") },
            text = { Text("Записанные подходы этого упражнения также будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveExercise(exerciseId)
                    exerciseToRemove = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRemove = null }) { Text("Отмена") }
            },
        )
    }

    if (showFinishWarning) {
        AlertDialog(
            onDismissRequest = { showFinishWarning = false },
            title = { Text("Завершить тренировку?") },
            text = {
                Text(
                    when {
                        workout?.exercises?.isEmpty() == true -> "В тренировке пока нет упражнений."
                        else -> "Не заполнено подходов: $incompleteSetCount."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishWarning = false
                    workout?.let { onFinish(it.id) }
                }) { Text("Завершить всё равно") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishWarning = false }) { Text("Продолжить тренировку") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(workout?.name ?: "Тренировка")
                        Text("Сохраняется на устройстве", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    TextButton(
                        enabled = workout != null,
                        onClick = {
                            workout?.let {
                                if (it.exercises.isEmpty() || incompleteSetCount > 0) {
                                    showFinishWarning = true
                                } else {
                                    onFinish(it.id)
                                }
                            }
                        },
                    ) { Text("Завершить") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (workout == null) {
                item { Text("Активная тренировка не найдена") }
            } else if (workout.exercises.isEmpty()) {
                item { Text("Пустая тренировка создана. Добавьте первое упражнение из каталога.") }
            } else {
                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    item(key = exercise.id) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("${exerciseIndex + 1}. ${exercise.name}", fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    TextButton(
                                        enabled = exerciseIndex > 0,
                                        onClick = { onMoveExercise(exercise.id, -1) },
                                    ) { Text("↑") }
                                    TextButton(
                                        enabled = exerciseIndex < workout.exercises.lastIndex,
                                        onClick = { onMoveExercise(exercise.id, 1) },
                                    ) { Text("↓") }
                                    TextButton(onClick = { exerciseToReplace = exercise.id }) { Text("Заменить") }
                                    TextButton(onClick = { exerciseToRemove = exercise.id }) { Text("Удалить") }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Цель: ${exercise.repMin}–${exercise.repMax} повторений")
                                Text(exercise.loadMode.loadHint())
                                Spacer(Modifier.height(16.dp))
                                exercise.sets.forEach { set ->
                                    SetInputRow(
                                        set = set,
                                        loadMode = exercise.loadMode,
                                        weightUnit = weightUnit,
                                        onSave = { load, reps, effort ->
                                            onSaveSet(set.id, load, reps, effort)
                                            if (restTimerAutoStart) startRestTimer(restDurationSeconds)
                                        },
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = { onAddSet(exercise.id) },
                                    ) { Text("+ подход") }
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        enabled = exercise.sets.size > 1,
                                        onClick = { onRemoveLastSet(exercise.id) },
                                    ) { Text("− последний") }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Таймер отдыха", fontWeight = FontWeight.Bold)
                                Text(if (restTimerAutoStart) "Автостарт после подхода" else "Запускается вручную")
                            }
                            Switch(
                                checked = restTimerAutoStart,
                                onCheckedChange = { restTimerAutoStart = it },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = formatRestDuration(restSecondsRemaining),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val updated = adjustRestDuration(restDurationSeconds, -30)
                                    restDurationSeconds = updated
                                    restSecondsRemaining = updated
                                    restTimerRunning = false
                                },
                            ) { Text("−30") }
                            OutlinedButton(
                                onClick = {
                                    val updated = adjustRestDuration(restDurationSeconds, 30)
                                    restDurationSeconds = updated
                                    restSecondsRemaining = updated
                                    restTimerRunning = false
                                },
                            ) { Text("+30") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (restTimerRunning) {
                                        restSecondsRemaining = remainingRestSeconds(
                                            restTimerEndMillis,
                                            System.currentTimeMillis(),
                                        )
                                        restTimerRunning = false
                                    } else {
                                        startRestTimer(
                                            restSecondsRemaining.takeIf { it > 0 } ?: restDurationSeconds,
                                        )
                                    }
                                },
                            ) { Text(if (restTimerRunning) "Пауза" else "Старт") }
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                restTimerRunning = false
                                restSecondsRemaining = restDurationSeconds
                            },
                        ) {
                            Text("Сбросить")
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = workout != null,
                    onClick = { showExercisePicker = true },
                ) {
                    Text("Добавить упражнение")
                }
            }
        }
    }
}

@Composable
private fun SetInputRow(
    set: WorkoutSet,
    loadMode: LoadMode,
    weightUnit: WeightUnit,
    onSave: (Long?, Int?, EffortMark) -> Unit,
) {
    var weight by rememberSaveable(set.id, weightUnit) { mutableStateOf(set.loadGrams?.toWeightText(weightUnit).orEmpty()) }
    var reps by rememberSaveable(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var effort by rememberSaveable(set.id) { mutableStateOf(set.effort) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${set.order + 1}", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
            if (loadMode != LoadMode.BODYWEIGHT) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(loadMode.loadLabel(weightUnit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.width(8.dp))
            }
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = reps,
                onValueChange = { reps = it.filter(Char::isDigit) },
                label = { Text("повт.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EffortChip("2+", EffortMark.RIR_2_PLUS, effort) { effort = it }
            EffortChip("1", EffortMark.RIR_1, effort) { effort = it }
            EffortChip("Отказ", EffortMark.FAILURE, effort) { effort = it }
        }
        Spacer(Modifier.height(6.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = (loadMode == LoadMode.BODYWEIGHT || weight.toWeightGrams(weightUnit) != null) && reps.toIntOrNull() != null,
            onClick = {
                onSave(
                    if (loadMode == LoadMode.BODYWEIGHT) null else weight.toWeightGrams(weightUnit),
                    reps.toIntOrNull(),
                    effort,
                )
            },
        ) {
            Text(if (set.completed) "Обновить подход" else "Сохранить подход")
        }
    }
}

private fun Long.toSignedWeightText(unit: WeightUnit): String =
    (if (this > 0) "+" else "") + toWeightText(unit)

private fun Long.toMeasurementDate(): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(this))

private fun LoadMode.loadLabel(unit: WeightUnit): String = when (this) {
    LoadMode.ASSISTANCE -> "помощь, ${unit.symbol}"
    LoadMode.BODYWEIGHT_ADDED -> "доп. ${unit.symbol}"
    else -> unit.symbol
}

private fun LoadMode.loadHint(): String = when (this) {
    LoadMode.DUMBBELL_PER_HAND -> "Указывается вес одной гантели"
    LoadMode.ASSISTANCE -> "Меньше помощи — выше сложность"
    LoadMode.BODYWEIGHT -> "Нагрузка — собственный вес"
    LoadMode.BODYWEIGHT_ADDED -> "Указывается только дополнительный вес"
    LoadMode.MACHINE -> "Указывается значение тренажёра"
    LoadMode.EXTERNAL_TOTAL -> "Указывается общий внешний вес"
}

@Composable
private fun EffortChip(
    label: String,
    value: EffortMark,
    selected: EffortMark,
    onSelect: (EffortMark) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(if (selected == value) EffortMark.UNKNOWN else value) },
        label = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    currentRoute: AppRoute,
    onNavigate: (AppRoute) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = {
            MainNavigationBar(
                current = currentRoute,
                onNavigate = onNavigate,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message)
        }
    }
}

@Composable
fun MainNavigationBar(
    current: AppRoute,
    onNavigate: (AppRoute) -> Unit,
) {
    NavigationBar {
        mainDestinations.forEach { destination ->
            NavigationBarItem(
                selected = current.route == destination.route,
                onClick = { if (current.route != destination.route) onNavigate(destination) },
                icon = { Text(destination.label.take(1)) },
                label = { Text(destination.label) },
            )
        }
    }
}

