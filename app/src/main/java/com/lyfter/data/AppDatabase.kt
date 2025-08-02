package com.lyfter.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lyfter.model.*

@Database(
    entities = [
        Exercise::class,
        Workout::class,
        WorkoutExercise::class,
        ExerciseSet::class,
        WorkoutPlan::class,
        WorkoutPlanExercise::class,
        PreExerciseSurvey::class,
        PostExerciseSurvey::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(com.lyfter.data.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun setDao(): SetDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun surveyDao(): SurveyDao
}
