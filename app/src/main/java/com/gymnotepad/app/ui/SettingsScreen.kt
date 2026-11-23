package com.gymnotepad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
    onNavigate: (AppRoute) -> Unit,
) {
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
            item { Text("Резервное копирование будет добавлено следующим этапом.") }
        }
    }
}
