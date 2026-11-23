package com.gymnotepad.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymnotepad.app.data.ExerciseDefinition
import com.gymnotepad.app.data.TemplateExercise
import com.gymnotepad.app.data.WorkoutTemplate
import com.gymnotepad.app.domain.LoadMode
import com.gymnotepad.app.domain.LoadProgressionType
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    templates: List<WorkoutTemplate>,
    exercises: List<ExerciseDefinition>,
    onCreateTemplate: (String) -> Unit,
    onCreateExercise: (String, LoadMode, LoadProgressionType, List<Long>, Long?) -> Unit,
    onUpdateLoadConfiguration: (String, LoadProgressionType, List<Long>, Long?) -> Unit,
    onAddExercise: (String, String) -> Unit,
    onUpdatePrescription: (String, Int, Int, Int) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    var selectedTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var templateName by rememberSaveable { mutableStateOf("") }
    var exerciseName by rememberSaveable { mutableStateOf("") }
    var selectedLoadMode by rememberSaveable { mutableStateOf(LoadMode.EXTERNAL_TOTAL) }
    var selectedProgression by rememberSaveable { mutableStateOf(LoadProgressionType.BARBELL_PLATES) }
    var availableLoads by rememberSaveable { mutableStateOf("") }
    var barWeight by rememberSaveable { mutableStateOf("20") }
    var configuringExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var configProgression by rememberSaveable { mutableStateOf(LoadProgressionType.CUSTOM_VALUES) }
    var configLoads by rememberSaveable { mutableStateOf("") }
    var configBarWeight by rememberSaveable { mutableStateOf("20") }
    val effectiveTemplateId = selectedTemplateId ?: templates.firstOrNull()?.id
    val selectedTemplate = templates.firstOrNull { it.id == effectiveTemplateId }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Шаблоны") }) },
        bottomBar = {
            MainNavigationBar(
                current = AppRoute.Templates,
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
                Text("Мои шаблоны", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    templates.forEach { template ->
                        FilterChip(
                            selected = template.id == effectiveTemplateId,
                            onClick = { selectedTemplateId = template.id },
                            label = { Text(template.name) },
                        )
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Новый шаблон", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Название") },
                            singleLine = true,
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = templateName.isNotBlank(),
                            onClick = {
                                onCreateTemplate(templateName)
                                templateName = ""
                            },
                        ) { Text("Создать шаблон") }
                    }
                }
            }

            item {
                Text(
                    selectedTemplate?.name ?: "Выберите шаблон",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (selectedTemplate?.exercises?.isEmpty() == true) {
                item { Text("В шаблоне пока нет упражнений") }
            }
            selectedTemplate?.let { template ->
                items(template.exercises, key = { it.id }) { exercise ->
                    TemplateExerciseCard(exercise, onUpdatePrescription)
                }
            }

            item {
                Text("Каталог упражнений", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(exercises, key = { it.id }) { exercise ->
                val alreadyAdded = selectedTemplate?.exercises?.any { it.exerciseId == exercise.id } == true
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, fontWeight = FontWeight.Bold)
                            Text(exercise.loadMode.displayName())
                        }
                        Column {
                            TextButton(
                                enabled = effectiveTemplateId != null && !alreadyAdded,
                                onClick = { effectiveTemplateId?.let { onAddExercise(it, exercise.id) } },
                            ) { Text(if (alreadyAdded) "Добавлено" else "Добавить") }
                            TextButton(
                                onClick = {
                                    configuringExerciseId = exercise.id
                                    configProgression = exercise.progressionType
                                    configLoads = exercise.availableLoadsGrams.joinToString("; ") { it.kgText() }
                                    configBarWeight = exercise.barWeightGrams?.kgText() ?: "20"
                                },
                            ) { Text("Настроить веса") }
                        }
                    }
                }
            }

            configuringExerciseId?.let { exerciseId ->
                val exercise = exercises.firstOrNull { it.id == exerciseId }
                if (exercise != null) {
                    item(key = "load-config-$exerciseId") {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Нагрузка: ${exercise.name}", fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    progressionChoices.forEach { progression ->
                                        FilterChip(
                                            selected = configProgression == progression,
                                            onClick = { configProgression = progression },
                                            label = { Text(progression.displayName()) },
                                        )
                                    }
                                }
                                if (configProgression == LoadProgressionType.CUSTOM_VALUES) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = configLoads,
                                        onValueChange = { configLoads = it },
                                        label = { Text("Доступные значения через ;") },
                                        supportingText = { Text("Например: 5; 10; 15; 22,5; 30") },
                                    )
                                }
                                if (configProgression == LoadProgressionType.BARBELL_PLATES) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = configBarWeight,
                                        onValueChange = { configBarWeight = it },
                                        label = { Text("Вес грифа, кг") },
                                        singleLine = true,
                                    )
                                }
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onUpdateLoadConfiguration(
                                            exerciseId,
                                            configProgression,
                                            configLoads.parseKgList(),
                                            if (configProgression == LoadProgressionType.BARBELL_PLATES) configBarWeight.toGrams() else null,
                                        )
                                        configuringExerciseId = null
                                    },
                                ) { Text("Сохранить схему нагрузки") }
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Новое упражнение", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = exerciseName,
                            onValueChange = { exerciseName = it },
                            label = { Text("Название упражнения") },
                            singleLine = true,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            loadModeChoices.forEach { mode ->
                                FilterChip(
                                    selected = selectedLoadMode == mode,
                                    onClick = { selectedLoadMode = mode },
                                    label = { Text(mode.displayName()) },
                                )
                            }
                        }
                        Text("Схема увеличения", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            progressionChoices.forEach { progression ->
                                FilterChip(
                                    selected = selectedProgression == progression,
                                    onClick = { selectedProgression = progression },
                                    label = { Text(progression.displayName()) },
                                )
                            }
                        }
                        if (selectedProgression == LoadProgressionType.CUSTOM_VALUES) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = availableLoads,
                                onValueChange = { availableLoads = it },
                                label = { Text("Доступные веса через ;") },
                                supportingText = { Text("Например: 5; 10; 15; 22,5; 30") },
                            )
                        }
                        if (selectedProgression == LoadProgressionType.BARBELL_PLATES) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = barWeight,
                                onValueChange = { barWeight = it },
                                label = { Text("Вес грифа, кг") },
                                singleLine = true,
                            )
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = exerciseName.isNotBlank(),
                            onClick = {
                                onCreateExercise(
                                    exerciseName,
                                    selectedLoadMode,
                                    selectedProgression,
                                    availableLoads.parseKgList(),
                                    if (selectedProgression == LoadProgressionType.BARBELL_PLATES) barWeight.toGrams() else null,
                                )
                                exerciseName = ""
                            },
                        ) { Text("Добавить в каталог") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateExerciseCard(
    exercise: TemplateExercise,
    onUpdate: (String, Int, Int, Int) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(exercise.exerciseName, fontWeight = FontWeight.Bold)
            Text(exercise.loadMode.displayName())
            Spacer(Modifier.height(8.dp))
            CounterRow("Рабочие подходы", exercise.workSetCount) { value ->
                onUpdate(exercise.id, value, exercise.repMin, exercise.repMax)
            }
            CounterRow("Минимум повторов", exercise.repMin) { value ->
                onUpdate(exercise.id, exercise.workSetCount, value, exercise.repMax)
            }
            CounterRow("Максимум повторов", exercise.repMax) { value ->
                onUpdate(exercise.id, exercise.workSetCount, exercise.repMin, value)
            }
        }
    }
}

@Composable
private fun CounterRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("$label: $value")
        Row {
            OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(1)) }) { Text("−") }
            OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
        }
    }
}

private val loadModeChoices = listOf(
    LoadMode.EXTERNAL_TOTAL,
    LoadMode.DUMBBELL_PER_HAND,
    LoadMode.MACHINE,
    LoadMode.BODYWEIGHT,
    LoadMode.BODYWEIGHT_ADDED,
    LoadMode.ASSISTANCE,
)

private val progressionChoices = listOf(
    LoadProgressionType.DUMBBELL_2_KG,
    LoadProgressionType.CUSTOM_VALUES,
    LoadProgressionType.BARBELL_PLATES,
    LoadProgressionType.PLATE_LOADED,
    LoadProgressionType.NONE,
)

private fun LoadMode.displayName(): String = when (this) {
    LoadMode.EXTERNAL_TOTAL -> "Общий вес"
    LoadMode.DUMBBELL_PER_HAND -> "Одна гантель"
    LoadMode.MACHINE -> "Тренажёр"
    LoadMode.BODYWEIGHT -> "Вес тела"
    LoadMode.BODYWEIGHT_ADDED -> "Вес тела +"
    LoadMode.ASSISTANCE -> "Помощь"
}

private fun LoadProgressionType.displayName(): String = when (this) {
    LoadProgressionType.DUMBBELL_2_KG -> "Гантели +2 кг"
    LoadProgressionType.CUSTOM_VALUES -> "Список весов"
    LoadProgressionType.BARBELL_PLATES -> "Штанга и блины"
    LoadProgressionType.PLATE_LOADED -> "Тренажёр под блины"
    LoadProgressionType.NONE -> "Без автоприбавки"
}

private fun String.parseKgList(): List<Long> = split(Regex("[;\\s]+"))
    .mapNotNull(String::toGrams)
    .distinct()
    .sorted()

private fun String.toGrams(): Long? = runCatching {
    replace(',', '.').toBigDecimalOrNull()?.multiply(BigDecimal(1_000))?.longValueExact()
}.getOrNull()

private fun Long.kgText(): String = BigDecimal.valueOf(this, 3).stripTrailingZeros().toPlainString()
