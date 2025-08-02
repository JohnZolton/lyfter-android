package com.lyfter.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutExerciseId"])
    ]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutExerciseId: Int,
    val setNumber: Int,
    val reps: Int,
    val weight: Double,
    val completed: Boolean = false,
    val notes: String = ""
)
