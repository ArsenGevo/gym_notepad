package com.gymnotepad.app.ui

sealed class AppRoute(val route: String, val label: String) {
    data object Home : AppRoute("home", "Главная")
    data object StartWorkout : AppRoute("start", "Начать")
    data object ActiveWorkout : AppRoute("active", "Тренировка")
    data object WorkoutSummary : AppRoute("summary", "Итоги")
    data object History : AppRoute("history", "История")
    data object Templates : AppRoute("templates", "Шаблоны")
    data object Settings : AppRoute("settings", "Настройки")
}

