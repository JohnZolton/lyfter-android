package com.lyfter.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String, // push, pull, legs
    val muscleGroup: String,
    val equipment: String,
    val description: String = ""
)
