package com.lyfter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.lyfter.model.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Int): Workout?

    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAll(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE category = :category ORDER BY cycleNumber DESC LIMIT 1")
    suspend fun getLatestByCategory(category: String): Workout?

    @Query("SELECT * FROM workouts WHERE category = :category AND cycleNumber = :cycleNumber")
    suspend fun getByCategoryAndCycle(category: String, cycleNumber: Int): Workout?

    @Query("SELECT COUNT(*) FROM workouts WHERE category = :category")
    suspend fun getCategoryCount(category: String): Int
}
