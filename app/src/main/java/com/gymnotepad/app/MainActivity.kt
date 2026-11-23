package com.gymnotepad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.gymnotepad.app.ui.GymNotepadApp
import com.gymnotepad.app.ui.WorkoutViewModel
import com.gymnotepad.app.ui.theme.GymNotepadTheme

class MainActivity : ComponentActivity() {
    private val workoutViewModel: WorkoutViewModel by viewModels {
        WorkoutViewModel.Factory(
            (application as GymNotepadApplication).workoutRepository,
            (application as GymNotepadApplication).userSettingsRepository,
            (application as GymNotepadApplication).backupRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymNotepadTheme {
                GymNotepadApp(workoutViewModel)
            }
        }
    }
}

