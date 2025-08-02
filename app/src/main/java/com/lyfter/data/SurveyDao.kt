package com.lyfter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lyfter.model.PostExerciseSurvey
import com.lyfter.model.PreExerciseSurvey

@Dao
interface SurveyDao {
    // Pre-exercise survey queries
    @Insert
    suspend fun insertPreExerciseSurvey(survey: PreExerciseSurvey): Long

    @Query("SELECT * FROM pre_exercise_surveys WHERE workoutId = :workoutId")
    suspend fun getPreExerciseSurvey(workoutId: Int): PreExerciseSurvey?

    // Post-exercise survey queries
    @Insert
    suspend fun insertPostExerciseSurvey(survey: PostExerciseSurvey): Long

    @Query("SELECT * FROM post_exercise_surveys WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun getPostExerciseSurvey(workoutExerciseId: Int): PostExerciseSurvey?

    @Query("SELECT * FROM post_exercise_surveys WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun getPostExerciseSurveysForWorkout(workoutId: Int): List<PostExerciseSurvey>
}