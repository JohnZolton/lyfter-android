package com.lyfter.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "workout_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["workoutPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutPlanId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutPlanExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutPlanId: Int,
    val exerciseId: Int,
    val orderIndex: Int,
    val targetSets: Int = 4,
    val targetReps: String = "9,8,7,6",
    val initialWeight: Double = 0.0,
    val restTime: Int = 90
)
