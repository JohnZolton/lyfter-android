package com.lyfter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.lyfter.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WorkoutExerciseDao {
    @Insert
    suspend fun insert(workoutExercise: WorkoutExercise): Long

    @Update
    suspend fun update(workoutExercise: WorkoutExercise)

    @Delete
    suspend fun delete(workoutExercise: WorkoutExercise)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getById(id: Int): WorkoutExercise?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex")
    fun getByWorkoutId(workoutId: Int): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getByWorkoutIdSync(workoutId: Int): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun getByWorkoutAndExercise(workoutId: Int, exerciseId: Int): WorkoutExercise?

    @Query("UPDATE workout_exercises SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun updateExerciseCompletion(id: Int, completed: Boolean, completedAt: Date?)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND completed = 0 ORDER BY orderIndex LIMIT 1")
    suspend fun getNextUncompletedExercise(workoutId: Int): WorkoutExercise?
}
