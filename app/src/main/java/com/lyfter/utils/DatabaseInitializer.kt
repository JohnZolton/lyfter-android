package com.lyfter.utils

import com.lyfter.model.Exercise
import com.lyfter.repository.WorkoutRepository

object DatabaseInitializer {
    suspend fun initializeDatabase(repository: WorkoutRepository) {
        // Add default exercises
        val defaultExercises = listOf(
            Exercise(name = "Bench Press", category = "push", muscleGroup = "Chest", equipment = "Barbell"),
            Exercise(name = "Overhead Press", category = "push", muscleGroup = "Shoulders", equipment = "Barbell"),
            Exercise(name = "Dips", category = "push", muscleGroup = "Chest", equipment = "Bodyweight"),
            Exercise(name = "Pull-ups", category = "pull", muscleGroup = "Back", equipment = "Bodyweight"),
            Exercise(name = "Barbell Rows", category = "pull", muscleGroup = "Back", equipment = "Barbell"),
            Exercise(name = "Deadlifts", category = "pull", muscleGroup = "Back", equipment = "Barbell"),
            Exercise(name = "Squats", category = "legs", muscleGroup = "Quads", equipment = "Barbell"),
            Exercise(name = "Romanian Deadlifts", category = "legs", muscleGroup = "Hamstrings", equipment = "Barbell"),
            Exercise(name = "Leg Press", category = "legs", muscleGroup = "Quads", equipment = "Machine")
        )
        
        defaultExercises.forEach { exercise ->
            repository.insertExercise(exercise)
        }
    }
}
