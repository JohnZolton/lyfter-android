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
    val showMicroDeload by viewModel.showSanctionNotification.collectAsStateWithLifecycle()
    
    // Survey states
    var showPreSurvey by remember { mutableStateOf(false) }
    var showExerciseSurvey by remember { mutableStateOf(false) }
    var surveyForExercise by remember { mutableStateOf<WorkoutExercise?>(null) }
    
    LaunchedEffect(workoutId) {
        // Load the specific workout by ID
        viewModel.loadWorkoutById(workoutId)
    }
    
    // Show pre-survey for non-first workout cycles
    LaunchedEffect(workout) {
        showPreSurvey = workout?.cycleNumber ?: 0 > 1
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
            onDismissRequest = { viewModel.dismissMicroDeloadNotificationSanction() },
            onContinue = { viewModel.dismissMicroDeloadNotificationSanction() }
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
                    Text("${workout?.name.orEmpty()} #${workout?.cycleNumber ?: ""}") 
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
                                workoutCategory = currentWorkout.category,
                                workoutcycleNumber = currentWorkout.cycleNumber ?: 0,
                                onSetUpdate = { setNumber, reps, weight ->
                                    viewModel.updateSet(workoutExercise.id, setNumber, reps, weight)
                                },
                                onCompareSet = { setNumber, reps, weight ->
                                    viewModel.compareSet(
                                        workoutExercise.exerciseId,
                                        currentWorkout.category,
                                        currentWorkout.cycleNumber ?: 0,
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
                                onMoveUp = {
                                    viewModel.moveExerciseUp(workoutExercise.id)
                                },
                                onMoveDown = {
                                    viewModel.moveExerciseDown(workoutExercise.id)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = offset.dp)
                                    .zIndex((exercises.size - index).toFloat())
                            )
                        }
                    }
                }
                
                // Exit workout button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.exitWorkout(workoutId)
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Exit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit Workout")
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
    workoutCategory: String,
    workoutcycleNumber: Int,
    onSetUpdate: (Int, Int, Double) -> Unit,
    onCompareSet: (Int, Int, Double) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onCompleteExercise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
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
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Set") },
                            onClick = {
                                onAddSet()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Set") },
                            onClick = {
                                onRemoveSet()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            onClick = {
                                onMoveUp()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            onClick = {
                                onMoveDown()
                                showMenu = false
                            }
                        )
                    }
                }
                
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
                    targetReps = workoutExercise.targetReps.split(",").first().toIntOrNull() ?: 10,
                    targetWeight = workoutExercise.targetWeight,
                    workoutcycleNumber = workoutcycleNumber,
                    onUpdate = { reps, weight ->
                        onSetUpdate(set.setNumber, reps, weight)
                        // Check if all sets for this exercise have values and trigger survey automatically
                        val allSetsHaveValues = sets.all { 
                            val setReps = if (it.setNumber == set.setNumber) reps else it.reps
                            val setWeight = if (it.setNumber == set.setNumber) weight else it.weight
                            setReps > 0 && setWeight > 0.0
                        }
                        if (allSetsHaveValues && sets.isNotEmpty()) {
                            onCompleteExercise()
                        }
                    },
                    onCompare = { reps, weight ->
                        onCompareSet(set.setNumber, reps, weight)
                    }
                )
            }
        }
    }
}

@Composable
fun SetRow(
    set: ExerciseSet,
    targetReps: Int,
    targetWeight: Double,
    workoutcycleNumber: Int,
    onUpdate: (Int, Double) -> Unit,
    onCompare: (Int, Double) -> Unit
) {
    // Create lists for dropdown options with more manageable ranges
    val repsOptions = (0..30).map { it.toString() }
    // Weight options in increments of 5 lbs from 0 to 500
    val weightOptions = (0..500).filter { it % 5 == 0 }.map { it.toString() }
    
    // Initialize selected values
    var selectedReps by remember(set.reps) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "0") }
    var selectedWeight by remember(set.weight) { mutableStateOf(if (set.weight > 0) set.weight.toInt().toString() else "0") }
    var comparisonResult by remember { mutableStateOf<ComparisonResult?>(null) }
    
    // Update selected values when set changes
    LaunchedEffect(set.reps) {
        selectedReps = if (set.reps > 0) set.reps.toString() else "0"
    }
    
    LaunchedEffect(set.weight) {
        selectedWeight = if (set.weight > 0) set.weight.toInt().toString() else "0"
    }
    
    // Dropdown states
    var repsExpanded by remember { mutableStateOf(false) }
    var weightExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column with "Set #{number}" text
        Text(
            text = "Set #${set.setNumber}",
            modifier = Modifier.width(60.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Weight dropdown with fixed width
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedWeight,
                onValueChange = { },
                label = { Text("Weight") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { weightExpanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand weight options")
                    }
                }
            )
            
            DropdownMenu(
                expanded = weightExpanded,
                onDismissRequest = { weightExpanded = false },
                modifier = Modifier.width(100.dp)
            ) {
                weightOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedWeight = option
                            weightExpanded = false
                            val repValue = selectedReps.toIntOrNull() ?: 0
                            val weightValue = option.toDoubleOrNull() ?: 0.0
                            onUpdate(repValue, weightValue)
                            onCompare(repValue, weightValue)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Reps dropdown with fixed width
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedReps,
                onValueChange = { },
                label = { Text("Reps") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { repsExpanded = true }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand reps options")
                    }
                }
            )
            
            DropdownMenu(
                expanded = repsExpanded,
                onDismissRequest = { repsExpanded = false },
                modifier = Modifier.width(100.dp)
            ) {
                repsOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedReps = option
                            repsExpanded = false
                            val repValue = option.toIntOrNull() ?: 0
                            val weightValue = selectedWeight.toDoubleOrNull() ?: 0.0
                            onUpdate(repValue, weightValue)
                            onCompare(repValue, weightValue)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Target column with target symbol (only show for workouts after the first cycle)
        if (workoutcycleNumber > 1) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = "Target",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${targetWeight.toInt()}×${targetReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // For first cycle, show empty column with fixed width
            Spacer(modifier = Modifier.width(80.dp))
        }
        
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
