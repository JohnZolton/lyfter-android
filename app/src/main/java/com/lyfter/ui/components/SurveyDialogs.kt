package com.lyfter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lyfter.model.RecoveryStatus
import com.lyfter.model.WorkoutIntensity
import com.lyfter.model.PumpRating

@Composable
fun PreExerciseSurveyDialog(
    onDismissRequest: () -> Unit,
    onSurveyComplete: (RecoveryStatus) -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Pre-Workout Check",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "How did you heal?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecoveryStatus.values().forEach { status ->
                        TextButton(
                            onClick = { onSurveyComplete(status) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = status.toDisplayString(), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostExerciseSurveyForExerciseDialog(
    exerciseName: String,
    onDismissRequest: () -> Unit,
    onSurveyComplete: (WorkoutIntensity, PumpRating) -> Unit
) {
    var intensitySelection by remember { mutableStateOf<WorkoutIntensity?>(null) }
    var pumpSelection by remember { mutableStateOf<PumpRating?>(null) }
    
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Post-Exercise Survey",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "How was your $exerciseName workout?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Intensity question
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How intense was that exercise?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    WorkoutIntensity.values().forEach { intensity ->
                        FilterChip(
                            selected = intensitySelection == intensity,
                            onClick = { intensitySelection = intensity },
                            label = { Text(intensity.toDisplayString()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Pump question
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rate your pump for $exerciseName:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    PumpRating.values().forEach { pump ->
                        FilterChip(
                            selected = pumpSelection == pump,
                            onClick = { pumpSelection = pump },
                            label = { Text(pump.toDisplayString()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (intensitySelection != null && pumpSelection != null) {
                            onSurveyComplete(intensitySelection!!, pumpSelection!!)
                        }
                    },
                    enabled = intensitySelection != null && pumpSelection != null,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save & Continue")
                }
            }
        }
    }
}

@Composable
fun MicroDeloadNotificationDialog(
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Auto Deload Detected") },
        text = {
            Text(
                "Your previous workout showed performance regression. " +
                "This workout has been automatically deloaded: 50% fewer sets and 75% of the previous weight.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Got it")
            }
        }
    )
}

private fun RecoveryStatus.toDisplayString(): String {
    return when (this) {
        RecoveryStatus.A_WHILE_AGO -> "a while ago"
        RecoveryStatus.JUST_IN_TIME -> "just in time"
        RecoveryStatus.STILL_SORE -> "still sore"
        RecoveryStatus.STILL_REALLY_SORE -> "still really sore"
    }
}

private fun WorkoutIntensity.toDisplayString(): String {
    return when (this) {
        WorkoutIntensity.EASY -> "easy"
        WorkoutIntensity.MODERATE -> "moderate"
        WorkoutIntensity.CHALLENGING -> "challenging"
        WorkoutIntensity.PUSHED_LIMITS -> "pushed my limits"
    }
}

private fun PumpRating.toDisplayString(): String {
    return when (this) {
        PumpRating.LOW -> "low"
        PumpRating.MEDIUM -> "medium"
        PumpRating.HIGH -> "high"
        PumpRating.OH_MY_GOD -> "oh my god"
    }
}