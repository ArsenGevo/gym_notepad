package com.gymnotepad.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_measurements ORDER BY measuredAt DESC, id DESC")
    fun observeMeasurements(): Flow<List<BodyWeightMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeasurement(measurement: BodyWeightMeasurementEntity)

    @Query("DELETE FROM body_weight_measurements WHERE id = :measurementId")
    suspend fun deleteMeasurement(measurementId: String)
}
