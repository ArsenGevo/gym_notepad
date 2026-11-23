package com.gymnotepad.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        WorkoutSetEntity::class,
        ExerciseEntity::class,
        ExerciseLoadOptionEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        BodyWeightMeasurementEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun templateDao(): TemplateDao
    abstract fun bodyWeightDao(): BodyWeightDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN exerciseDefinitionId TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS exercises (id TEXT NOT NULL, name TEXT NOT NULL, loadMode TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_templates (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS template_exercises (id TEXT NOT NULL, templateId TEXT NOT NULL, exerciseId TEXT NOT NULL, sortOrder INTEGER NOT NULL, workSetCount INTEGER NOT NULL, repMin INTEGER NOT NULL, repMax INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_templateId ON template_exercises(templateId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseId ON template_exercises(exerciseId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN progressionType TEXT NOT NULL DEFAULT 'CUSTOM_VALUES'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN barWeightGrams INTEGER")
                db.execSQL("UPDATE exercises SET progressionType = 'DUMBBELL_2_KG' WHERE loadMode = 'DUMBBELL_PER_HAND'")
                db.execSQL("UPDATE exercises SET progressionType = 'NONE' WHERE loadMode = 'BODYWEIGHT'")
                db.execSQL("CREATE TABLE IF NOT EXISTS exercise_load_options (id TEXT NOT NULL, exerciseId TEXT NOT NULL, loadGrams INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_load_options_exerciseId_loadGrams ON exercise_load_options(exerciseId, loadGrams)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS body_weight_measurements (id TEXT NOT NULL, measuredAt INTEGER NOT NULL, weightGrams INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_body_weight_measurements_measuredAt ON body_weight_measurements(measuredAt)")
            }
        }
    }
}
