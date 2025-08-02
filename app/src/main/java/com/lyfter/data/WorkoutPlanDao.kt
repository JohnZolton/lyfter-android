package com.lyfter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.lyfter.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Insert
    suspend fun insert(workoutPlan: WorkoutPlan): Long

    @Update
    suspend fun update(workoutPlan: WorkoutPlan)

    @Delete
    suspend fun delete(workoutPlan: WorkoutPlan)

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getById(id: Int): WorkoutPlan?

    @Query("SELECT * FROM workout_plans ORDER BY name")
    fun getAll(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE isActive = 1")
    fun getActivePlans(): Flow<List<WorkoutPlan>>
}
