package com.lyfter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lyfter.LyfterApplication
import com.lyfter.model.Workout
import com.lyfter.ui.viewmodel.WorkoutPlanViewModel
import com.lyfter.ui.viewmodel.WorkoutPlanViewModelFactory
import kotlin.OptIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: WorkoutPlanViewModel = viewModel(
        factory = WorkoutPlanViewModelFactory(
            LocalContext.current.applicationContext as LyfterApplication
        )
    )
) {
    val activeWorkoutPlan by viewModel.activeWorkoutPlan.collectAsState()
    val nextWorkout by viewModel.nextWorkout.collectAsState()
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lyfter") },
                actions = {
                    IconButton(onClick = {
                        showMenu = true
                    }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (activeWorkoutPlan != null) {
                            DropdownMenuItem(
                                text = { Text("Modify Workout Plan") },
                                onClick = {
                                    navController.navigate("edit-workout-plan/${activeWorkoutPlan!!.id}")
                                    showMenu = false
                                }
                            )
                            Divider()
                        }
                        DropdownMenuItem(
                            text = { Text("Create New Workout Plan") },
                            onClick = {
                                navController.navigate("create-workout-plan")
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeWorkoutPlan == null) {
                // No workout plan - show create button
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No workout plan found",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = { navController.navigate("create-workout-plan") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            Text("Create Workout Plan")
                        }
                    }
                }
            } else {
                // Has workout plan - show current workout at top and upcoming workouts below
                // Sort workouts by cycle number
                val sortedWorkouts = allWorkouts.sortedBy { it.cycleNumber }
                
                // Display the current workout (next workout that is not completed) at the top
                nextWorkout?.let { currentWorkout ->
                    Text(
                        text = "Current Workout",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.Start)
                    )
                    
                    WorkoutCard(
                        workout = currentWorkout,
                        isNext = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        onStartWorkout = { 
                            navController.navigate("workout/${currentWorkout.id}") 
                        }
                    )
                }
                
                // Display upcoming workouts below (grayed out)
                if (allWorkouts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No workouts in this plan",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    Text(
                        text = "Upcoming Workouts",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .align(Alignment.Start)
                    )
                    
                    // Display upcoming workouts in a list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(sortedWorkouts.filter { it.id != nextWorkout?.id }) { workout ->
                            WorkoutCard(
                                workout = workout,
                                isNext = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                onStartWorkout = { 
                                    navController.navigate("workout/${workout.id}") 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(
    workout: Workout,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    onStartWorkout: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) 
                MaterialTheme.colorScheme.primaryContainer 
            else if (workout.completed)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                if (isNext) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "NEXT",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else if (workout.completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Text(
                text = "Cycle: ${workout.cycleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Text(
                text = if (workout.completed) "Completed" else "Pending",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
                color = if (workout.completed) 
                    MaterialTheme.colorScheme.onSecondaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
                if (!workout.completed) {
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Start Workout")
                    }
                }
        }
    }
}
