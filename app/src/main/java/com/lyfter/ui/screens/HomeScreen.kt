package com.lyfter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lyfter.LyfterApplication
import com.lyfter.model.Workout
import com.lyfter.ui.viewmodel.WorkoutPlanViewModel
import com.lyfter.ui.viewmodel.WorkoutPlanViewModelFactory
import kotlinx.coroutines.launch

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
                // Has workout plan - show workout stack
                Text(
                    text = "Your Workout Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.Start)
                )
                
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
                    // Sort workouts by sequence number
                    val sortedWorkouts = allWorkouts.sortedBy { it.sequenceNumber }
                    
                    // Display workouts as a stack of cards
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        sortedWorkouts.forEachIndexed { index, workout ->
                            // Calculate offset for stacking effect (first item on top)
                            val offset = index * 16 // 16dp offset per card
                            
                            WorkoutCard(
                                workout = workout,
                                isNext = workout.id == nextWorkout?.id,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = offset.dp)
                                    .zIndex((sortedWorkouts.size - index).toFloat()),
                                onStartWorkout = { 
                                    navController.navigate("workout/${workout.id}") 
                                },
                                onClick = {
                                    navController.navigate("workout/${workout.id}")
                                }
                            )
                        }
                    }
                }
                
                // Show next workout info and actions at the bottom
                nextWorkout?.let { workout ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Next Up: ${workout.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { navController.navigate("workout/${workout.category}") },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                ) {
                                    Text("Start")
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.skipNextWorkout() },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                ) {
                                    Text("Skip")
                                }
                            }
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
                text = "Sequence: ${workout.sequenceNumber}",
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
            
            if (!workout.completed && !isNext) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    enabled = !isNext // Only enable if it's not already the next one
                ) {
                    Text("Start Workout")
                }
            }
        }
    }
}
