package com.gymnotepad.app

import android.app.Application
import androidx.room.Room
import com.gymnotepad.app.data.RoomWorkoutRepository
import com.gymnotepad.app.data.WorkoutRepository
import com.gymnotepad.app.data.local.GymDatabase

class GymNotepadApplication : Application() {
    val workoutRepository: WorkoutRepository by lazy {
        val database = Room.databaseBuilder(
            applicationContext,
            GymDatabase::class.java,
            "gym-notepad.db",
        ).addMigrations(
            GymDatabase.MIGRATION_1_2,
            GymDatabase.MIGRATION_2_3,
            GymDatabase.MIGRATION_3_4,
        ).build()
        RoomWorkoutRepository(database)
    }
}
