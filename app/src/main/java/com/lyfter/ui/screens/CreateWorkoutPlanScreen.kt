@file:OptIn(ExperimentalMaterial3Api::class)

package com.lyfter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import kotlin.math.max
import kotlin.math.min
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lyfter.LyfterApplication
import com.lyfter.ui.viewmodel.WorkoutPlanViewModel
import com.lyfter.ui.viewmodel.WorkoutPlanViewModelFactory
import com.lyfter.ui.viewmodel.WorkoutWithExercisesData
import com.lyfter.ui.viewmodel.ExerciseData
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun CreateWorkoutPlanScreen(
    navController: NavController,
    planId: Int?,
    viewModel: WorkoutPlanViewModel = viewModel(
        factory = WorkoutPlanViewModelFactory(
            LocalContext.current.applicationContext as LyfterApplication
        )
    )
) {
    var planName by remember { mutableStateOf("") }
    var planDescription by remember { mutableStateOf("") }
    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<WorkoutWithExercises?>(null) }
    
    val workouts = remember { mutableStateListOf<WorkoutWithExercises>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // For now, we won't load existing data - this will be a simple edit capability
    // In a full implementation, we'd need proper data loading
    LaunchedEffect(planId) {
        // Just ensure we're in edit mode when planId is provided
    }

    val screenTitle = if (planId == null) "Create Workout Plan" else "Edit Workout Plan"
    val buttonText = if (planId == null) "Create Plan" else "Save Changes"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Plan Details
        OutlinedTextField(
            value = planName,
            onValueChange = { planName = it },
            label = { Text("Plan Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = planDescription,
            onValueChange = { planDescription = it },
            label = { Text("Description (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Workouts List
        Text(
            text = "Workouts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(workouts) { index, workout ->
                WorkoutCard(
                    workout = workout,
                    onDelete = { workouts.removeAt(index) },
                    onMoveUp = { if (index > 0) workouts.swap(index, index - 1) },
                    onMoveDown = { if (index < workouts.size - 1) workouts.swap(index, index + 1) },
                    onAddExercise = {
                        selectedWorkout = workout
                        showAddExerciseDialog = true
                    }
                )
            }
        }

        // Add Workout Button
        Button(
            onClick = { showAddWorkoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Add Workout")
        }

        // Save Button
        Button(
            onClick = {
                viewModel.viewModelScope.launch {
                    val workoutList = workouts.map { workoutWithExercises ->
                        WorkoutWithExercisesData(
                            name = workoutWithExercises.name,
                            category = "Workout", // Default category, can be customized later
                            exercises = workoutWithExercises.exercises.map { exercise ->
                                ExerciseData(
                                    name = exercise.name,
                                    muscleGroup = exercise.muscleGroup,
                                    sets = exercise.sets
                                )
                            }
                        )
                    }
                    
                    // For now, we always create a new plan when editing too
                    // This needs to be enhanced in the future
                    viewModel.createWorkoutPlan(planName, planDescription, workoutList)
                    
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = planName.isNotEmpty()
        ) {
            Text(buttonText)
        }
    }

    // Add Workout Dialog
    if (showAddWorkoutDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddWorkoutDialog = false },
            onAddWorkout = { name ->
                workouts.add(WorkoutWithExercises(name, mutableStateListOf()))
                showAddWorkoutDialog = false
            }
        )
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog && selectedWorkout != null) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAddExercise = { name, muscleGroup, sets ->
                selectedWorkout?.exercises?.add(
                    Exercise(
                        name = name,
                        muscleGroup = muscleGroup,
                        sets = sets,
                        description = ""
                    )
                )
                showAddExerciseDialog = false
                selectedWorkout = null
            }
        )
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutWithExercises,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddExercise: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Row {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Exercises list
            if (workout.exercises.isNotEmpty()) {
                Text(
                    text = "Exercises:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                workout.exercises.forEach { exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onDelete = { workout.exercises.remove(exercise) },
                        onMoveUp = { 
                            val index = workout.exercises.indexOf(exercise)
                            if (index > 0) workout.exercises.swap(index, index - 1)
                        },
                        onMoveDown = { 
                            val index = workout.exercises.indexOf(exercise)
                            if (index < workout.exercises.size - 1) workout.exercises.swap(index, index + 1)
                        }
                    )
                }
            }
            
            Button(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Exercise")
            }
        }
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${exercise.muscleGroup} - ${exercise.sets} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onAddWorkout: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Workout") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workout Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddWorkout(name) },
                enabled = name.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAddExercise: (String, String, Int) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("Chest") }
    var sets by remember { mutableStateOf(3) }
    val muscleGroups = listOf(
        "Chest", "Triceps", "Biceps", "Traps", "Back", "Lats", 
        "Shoulders", "Abs", "Obliques", "Quads", "Hamstrings", "Calves", "Glutes", "Forearms"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Sets: $sets", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { sets = maxOf(1, sets - 1) },
                        enabled = sets > 1
                    ) {
                        Text("-")
                    }
                    Text(
                        text = "$sets",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { sets = minOf(5, sets + 1) },
                        enabled = sets < 5
                    ) {
                        Text("+")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Muscle Group:", style = MaterialTheme.typography.bodyMedium)
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = muscleGroup,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        muscleGroups.forEach { muscle ->
                            DropdownMenuItem(
                                text = { Text(muscle) },
                                onClick = {
                                    muscleGroup = muscle
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddExercise(exerciseName, muscleGroup, sets) },
                enabled = exerciseName.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val temp = this[index1]
    this[index1] = this[index2]
    this[index2] = temp
}

data class WorkoutWithExercises(
    val name: String,
    val exercises: MutableList<Exercise>
)

data class Exercise(
    val name: String,
    val muscleGroup: String,
    val sets: Int = 3,
    val description: String = ""
)
