package com.lyfter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.lyfter.model.ExerciseSet
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert
    suspend fun insert(set: ExerciseSet): Long

    @Update
    suspend fun update(set: ExerciseSet)

    @Delete
    suspend fun delete(set: ExerciseSet)

    @Query("SELECT * FROM exercise_sets WHERE id = :id")
    suspend fun getById(id: Int): ExerciseSet?

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    fun getByWorkoutExerciseId(workoutExerciseId: Int): Flow<List<ExerciseSet>>

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    suspend fun getByWorkoutExerciseIdSync(workoutExerciseId: Int): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId AND setNumber = :setNumber")
    suspend fun getByWorkoutExerciseAndSetNumber(workoutExerciseId: Int, setNumber: Int): ExerciseSet?

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun getAllSetsForWorkout(workoutId: Int): List<ExerciseSet>

    @Query("DELETE FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId AND setNumber = :setNumber")
    suspend fun deleteSetFromWorkoutExercise(workoutExerciseId: Int, setNumber: Int)
}
