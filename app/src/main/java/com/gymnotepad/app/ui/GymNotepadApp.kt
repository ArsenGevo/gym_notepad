package com.gymnotepad.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun GymNotepadApp(viewModel: WorkoutViewModel) {
    val navController = rememberNavController()
    val activeWorkout = viewModel.activeWorkout.collectAsStateWithLifecycle().value
    val templates = viewModel.templates.collectAsStateWithLifecycle().value
    val exercises = viewModel.exercises.collectAsStateWithLifecycle().value
    val history = viewModel.history.collectAsStateWithLifecycle().value
    val progress = viewModel.progress.collectAsStateWithLifecycle().value
    val recommendations = viewModel.recommendations.collectAsStateWithLifecycle().value
    val bodyWeight = viewModel.bodyWeight.collectAsStateWithLifecycle().value
    val lastCompletedSessionId = viewModel.lastCompletedSessionId.collectAsStateWithLifecycle().value

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                activeWorkout = activeWorkout,
                lastWorkout = history.firstOrNull(),
                bodyWeight = bodyWeight,
                onSaveBodyWeight = viewModel::saveBodyWeight,
                onDeleteBodyWeight = viewModel::deleteBodyWeight,
                onStartWorkout = { navController.navigate(AppRoute.StartWorkout.route) },
                onContinueWorkout = { navController.navigate(AppRoute.ActiveWorkout.route) },
                onNavigate = { route -> navController.navigate(route.route) },
            )
        }
        composable(AppRoute.StartWorkout.route) {
            StartWorkoutScreen(
                templates = templates,
                onBack = { navController.popBackStack() },
                onStartTemplate = { templateId ->
                    viewModel.startTemplate(templateId) { navController.navigate(AppRoute.ActiveWorkout.route) }
                },
                onStartPrevious = {
                    viewModel.startPrevious { navController.navigate(AppRoute.ActiveWorkout.route) }
                },
                onStartEmpty = {
                    viewModel.startEmpty { navController.navigate(AppRoute.ActiveWorkout.route) }
                },
            )
        }
        composable(AppRoute.ActiveWorkout.route) {
            ActiveWorkoutScreen(
                workout = activeWorkout,
                exercises = exercises,
                onSaveSet = viewModel::saveSet,
                onAddExercise = viewModel::addExerciseToActiveWorkout,
                onReplaceExercise = viewModel::replaceActiveExercise,
                onRemoveExercise = viewModel::removeExerciseFromActiveWorkout,
                onMoveExercise = viewModel::moveActiveExercise,
                onAddSet = viewModel::addSetToActiveExercise,
                onRemoveLastSet = viewModel::removeLastSetFromActiveExercise,
                onFinish = { sessionId ->
                    viewModel.completeWorkout(sessionId) {
                        navController.navigate(AppRoute.WorkoutSummary.route) {
                            popUpTo(AppRoute.ActiveWorkout.route) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(AppRoute.WorkoutSummary.route) {
            val summaryWorkout = history.firstOrNull { it.id == lastCompletedSessionId }
            WorkoutSummaryScreen(
                workout = summaryWorkout,
                recommendations = if (history.firstOrNull()?.id == lastCompletedSessionId) recommendations else emptyList(),
                onDone = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoute.History.route) {
            HistoryScreen(
                history = history,
                progress = progress,
                onNavigate = { navController.navigate(it.route) },
            )
        }
        composable(AppRoute.Templates.route) {
            TemplatesScreen(
                templates = templates,
                exercises = exercises,
                onCreateTemplate = viewModel::createTemplate,
                onCreateExercise = viewModel::createExercise,
                onUpdateLoadConfiguration = viewModel::updateExerciseLoadConfiguration,
                onAddExercise = viewModel::addExerciseToTemplate,
                onUpdatePrescription = viewModel::updateTemplateExercise,
                onNavigate = { navController.navigate(it.route) },
            )
        }
        composable(AppRoute.Settings.route) {
            PlaceholderScreen(
                title = "Настройки",
                message = "Единицы, таймер, уведомления и резервная копия.",
                currentRoute = AppRoute.Settings,
                onNavigate = { navController.navigate(it.route) },
            )
        }
    }
}

