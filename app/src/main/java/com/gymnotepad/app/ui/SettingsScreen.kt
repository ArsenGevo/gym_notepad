package com.gymnotepad.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymnotepad.app.data.UserSettings
import com.gymnotepad.app.data.WeightUnit
import com.gymnotepad.app.domain.adjustRestDuration
import com.gymnotepad.app.domain.formatRestDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onWeightUnitChange: (WeightUnit) -> Unit,
    onRestSecondsChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    backupMessage: String?,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onMessageShown: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    var confirmImport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportBackup)
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Восстановить резервную копию?") },
            text = { Text("Текущие тренировки, шаблоны и измерения веса будут заменены данными из файла.") },
            confirmButton = { Button(onClick = { confirmImport = false; importLauncher.launch(arrayOf("application/json")) }) { Text("Выбрать файл") } },
            dismissButton = { OutlinedButton(onClick = { confirmImport = false }) { Text("Отмена") } },
        )
    }
    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onMessageShown,
            title = { Text("Резервная копия") },
            text = { Text(message) },
            confirmButton = { Button(onClick = onMessageShown) { Text("Готово") } },
        )
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) },
        bottomBar = { MainNavigationBar(AppRoute.Settings, onNavigate) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Единицы веса", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(settings.weightUnit == WeightUnit.KILOGRAMS, { onWeightUnitChange(WeightUnit.KILOGRAMS) }, label = { Text("Килограммы") })
                            FilterChip(settings.weightUnit == WeightUnit.POUNDS, { onWeightUnitChange(WeightUnit.POUNDS) }, label = { Text("Фунты") })
                        }
                        Text("Хранение остаётся точным в граммах; меняются только ввод и отображение.")
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Таймер отдыха", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatRestDuration(settings.restSeconds), style = MaterialTheme.typography.headlineMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onRestSecondsChange(adjustRestDuration(settings.restSeconds, -30)) }) { Text("−30") }
                            OutlinedButton(onClick = { onRestSecondsChange(adjustRestDuration(settings.restSeconds, 30)) }) { Text("+30") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Автозапуск после подхода")
                            Switch(settings.autoStartRestTimer, onAutoStartChange)
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Резервная копия", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("JSON-файл содержит тренировки, шаблоны, упражнения, вес тела и настройки.")
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { exportLauncher.launch("gym-notepad-backup.json") }) { Text("Экспортировать") }
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { confirmImport = true }) { Text("Восстановить из файла") }
                    }
                }
            }
        }
    }
}
