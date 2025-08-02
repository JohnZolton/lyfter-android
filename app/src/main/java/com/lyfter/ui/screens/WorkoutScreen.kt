package com.lyfter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lyfter.LyfterApplication
import com.lyfter.model.ExerciseSet
import com.lyfter.model.WorkoutExercise
import com.lyfter.model.RecoveryStatus
import com.lyfter.model.WorkoutIntensity
import com.lyfter.model.PumpRating
import com.lyfter.repository.ComparisonResult
import com.lyfter.ui.components.PreExerciseSurveyDialog
import com.lyfter.ui.components.PostExerciseSurveyForExerciseDialog
import com.lyfter.ui.components.MicroDeloadNotificationDialog
import com.lyfter.ui.theme.BlueInfo
import com.lyfter.ui.theme.GreenSuccess
import com.lyfter.ui.theme.OrangeWarning
import com.lyfter.ui.theme.RedError
import com.lyfter.ui.viewmodel.WorkoutViewModel
import com.lyfter.ui.viewmodel.WorkoutViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    workoutId: Int,
    viewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModelFactory(
            LocalContext.current.applicationContext as LyfterApplication
        )
    )
) {
    val workout by viewModel.currentWorkout.collectAsStateWithLifecycle()
    val exercises by viewModel.workoutExercises.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val preSurvey by viewModel.preExerciseSurvey.collectAsStateWithLifecycle()
    val showMicroDeload by viewModel.showMicroDeloadNotification.collectAsStateWithLifecycle()
    
    // Survey states
    var showPreSurvey by remember { mutableStateOf(false) }
    var showExerciseSurvey by remember { mutableStateOf(false) }
    var surveyForExercise by remember { mutableStateOf<WorkoutExercise?>(null) }
    
    LaunchedEffect(workoutId) {
        // First get the workout to get its category
        val workout = viewModel.getWorkoutById(workoutId)
        if (workout != null) {
            viewModel.loadWorkout(workout.category)
            // Only show pre-survey for non-first workout cycles
            if (workout.sequenceNumber > 1) {
                showPreSurvey = true
            }
        }
    }

    // Pre-exercise survey dialog
    if (showPreSurvey && preSurvey == null) {
        PreExerciseSurveyDialog(
            onDismissRequest = { showPreSurvey = false },
            onSurveyComplete = { recoveryStatus ->
                viewModel.savePreExerciseSurvey(recoveryStatus)
                showPreSurvey = false
            }
        )
    }

    // Micro deload notification
    if (showMicroDeload) {
        MicroDeloadNotificationDialog(
            onDismissRequest = { viewModel.dismissMicroDeloadNotification() },
            onContinue = { viewModel.dismissMicroDeloadNotification() }
        )
    }

    // Post-exercise survey dialog for individual exercise
    surveyForExercise?.let { exercise ->
        PostExerciseSurveyForExerciseDialog(
            exerciseName = exercise.exerciseName,
            onDismissRequest = {
                surveyForExercise = null
                showExerciseSurvey = false
            },
            onSurveyComplete = { intensity, pumpRating ->
                viewModel.savePostExerciseSurvey(exercise.id, intensity, pumpRating)
                surveyForExercise = null
                showExerciseSurvey = false
                
                // Check if all exercises are completed
                viewModel.completeWorkoutIfAllExercisesDone(workout?.id ?: 0)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("${workout?.name.orEmpty()} #${workout?.sequenceNumber ?: ""}") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            workout?.let { currentWorkout ->
                // Survey status indicators
                var completedExercises = 0
                exercises.forEach { exercise ->
                    val exerciseSurveys = sets.filter { it.workoutExerciseId == exercise.id }
                        .mapNotNull { 
                            // For simplicity, we'll consider an exercise completed when user fills survey
                            true // Actual survey check would be from repository
                        }
                    if (exercise.completed) completedExercises++
                }
                
                // Display exercises as a stack of cards
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (exercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No exercises in this workout")
                        }
                    } else {
                        exercises.forEachIndexed { index, workoutExercise ->
                            val exerciseSets = sets.filter { it.workoutExerciseId == workoutExercise.id }
                            val isCompleted = workoutExercise.completed
                            
                            // Calculate offset for stacking effect (first item on top)
                            val offset = (exercises.size - 1 - index) * 16 // 16dp offset per card
                            
                            ExerciseCard(
                                workoutExercise = workoutExercise,
                                sets = exerciseSets,
                                isCompleted = isCompleted,
                                onSetUpdate = { setNumber, reps, weight ->
                                    viewModel.updateSet(workoutExercise.id, setNumber, reps, weight)
                                },
                                onCompareSet = { setNumber, reps, weight ->
                                    viewModel.compareSet(
                                        workoutExercise.exerciseId,
                                        currentWorkout.category,
                                        currentWorkout.sequenceNumber ?: 0,
                                        setNumber,
                                        reps,
                                        weight
                                    )
                                },
                                onAddSet = {
                                    viewModel.addSet(workoutExercise.id, exerciseSets)
                                },
                                onRemoveSet = {
                                    viewModel.removeSet(workoutExercise.id, exerciseSets)
                                },
                                onCompleteExercise = {
                                    // Trigger survey for this specific exercise
                                    surveyForExercise = workoutExercise
                                    showExerciseSurvey = true
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = offset.dp)
                                    .zIndex((exercises.size - index).toFloat())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    workoutExercise: WorkoutExercise,
    sets: List<ExerciseSet>,
    isCompleted: Boolean,
    onSetUpdate: (Int, Int, Double) -> Unit,
    onCompareSet: (Int, Int, Double) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onCompleteExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workoutExercise.exerciseName,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            sets.forEach { set ->
                SetRow(
                    set = set,
                    onUpdate = { reps, weight ->
                        onSetUpdate(set.setNumber, reps, weight)
                    },
                    onCompare = { reps, weight ->
                        onCompareSet(set.setNumber, reps, weight)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddSet,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Add Set")
                }
                
                Button(
                    onClick = onRemoveSet,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = sets.isNotEmpty()
                ) {
                    Text("Remove Set")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onCompleteExercise,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) 
                        MaterialTheme.colorScheme.secondary 
                    else 
                        MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(if (isCompleted) "Survey Complete" else "Complete Exercise & Survey")
            }
        }
    }
}

@Composable
fun SetRow(
    set: ExerciseSet,
    onUpdate: (Int, Double) -> Unit,
    onCompare: (Int, Double) -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var weight by remember { mutableStateOf(set.weight.toString()) }
    var comparisonResult by remember { mutableStateOf<ComparisonResult?>(null) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Set ${set.setNumber}",
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { 
                weight = it
                reps.toIntOrNull()?.let { repValue ->
                    it.toDoubleOrNull()?.let { weightValue ->
                        onUpdate(repValue, weightValue)
                        onCompare(repValue, weightValue)
                    }
                }
            },
            label = { Text("Weight") },
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        OutlinedTextField(
            value = reps,
            onValueChange = { 
                reps = it
                it.toIntOrNull()?.let { repValue ->
                    weight.toDoubleOrNull()?.let { weightValue ->
                        onUpdate(repValue, weightValue)
                        onCompare(repValue, weightValue)
                    }
                }
            },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f)
        )
        
        
        comparisonResult?.let { result ->
            Icon(
                imageVector = when (result) {
                    ComparisonResult.IMPROVED -> Icons.Default.KeyboardArrowUp
                    ComparisonResult.REGRESSED -> Icons.Default.KeyboardArrowDown
                    ComparisonResult.SAME -> Icons.Default.Minimize
                    ComparisonResult.NO_PREVIOUS -> Icons.Default.Info
                },
                contentDescription = "Comparison status",
                tint = when (result) {
                    ComparisonResult.IMPROVED -> GreenSuccess
                    ComparisonResult.REGRESSED -> RedError
                    ComparisonResult.SAME -> OrangeWarning
                    ComparisonResult.NO_PREVIOUS -> BlueInfo
                }
            )
        }
    }
}
