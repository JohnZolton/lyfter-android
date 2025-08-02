package com.lyfter.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date
import com.lyfter.model.Workout
import com.lyfter.model.WorkoutExercise

@Entity(
    tableName = "pre_exercise_surveys",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("workoutId")]
)
data class PreExerciseSurvey(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutId: Int,
    val recoveryStatus: RecoveryStatus,
    val date: Date = Date()
)

@Entity(
    tableName = "post_exercise_surveys",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("workoutExerciseId")]
)
data class PostExerciseSurvey(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workoutExerciseId: Int,
    val intensity: WorkoutIntensity,
    val pumpRating: PumpRating,
    val date: Date = Date()
)

enum class RecoveryStatus {
    A_WHILE_AGO,    // "a while ago"
    JUST_IN_TIME,   // "just in time"
    STILL_SORE,     // "still sore"
    STILL_REALLY_SORE // "still really sore"
}

enum class WorkoutIntensity {
    EASY,           // "easy"
    MODERATE,       // "moderate"
    CHALLENGING,    // "challenging"
    PUSHED_LIMITS   // "pushed my limits"
}

enum class PumpRating {
    LOW,            // "low"
    MEDIUM,         // "medium"
    HIGH,           // "high"
    OH_MY_GOD       // "oh my god"
}
