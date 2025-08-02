package com.lyfter.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val category: String, // push, pull, legs
    val sequenceNumber: Int, // 1, 2, 3, etc.
    val date: Date = Date(),
    val completed: Boolean = false
)
