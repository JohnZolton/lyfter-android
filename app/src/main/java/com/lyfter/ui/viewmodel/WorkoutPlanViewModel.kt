package com.lyfter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyfter.LyfterApplication
import com.lyfter.model.WorkoutPlan
import com.lyfter.model.Workout
import com.lyfter.model.Exercise
import com.lyfter.model.WorkoutExercise
import com.lyfter.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class WorkoutPlanViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _workoutPlans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val workoutPlans: StateFlow<List<WorkoutPlan>> = _workoutPlans.asStateFlow()
    
    private val _activeWorkoutPlan = MutableStateFlow<WorkoutPlan?>(null)
    val activeWorkoutPlan: StateFlow<WorkoutPlan?> = _activeWorkoutPlan.asStateFlow()
    
    private val _nextWorkout = MutableStateFlow<Workout?>(null)
    val nextWorkout: StateFlow<Workout?> = _nextWorkout.asStateFlow()
    
    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val allWorkouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()
    
    private val _workoutsForActivePlan = MutableStateFlow<List<Workout>>(emptyList())
    val workoutsForActivePlan: StateFlow<List<Workout>> = _workoutsForActivePlan.asStateFlow()

    init {
        loadWorkoutPlans()
        loadAllWorkouts()
    }

    private fun loadWorkoutPlans() {
        viewModelScope.launch {
            repository.getAllWorkoutPlans().collect { plans ->
                _workoutPlans.value = plans
                _activeWorkoutPlan.value = plans.find { it.isActive }
            }
        }
    }

    private fun loadAllWorkouts() {
        viewModelScope.launch {
            repository.getAllWorkouts().first().let { workouts ->
                _allWorkouts.value = workouts
                updateNextWorkout()
            }
        }
    }

    private fun updateNextWorkout() {
        viewModelScope.launch {
            val workouts = repository.getAllWorkouts().first()
            _allWorkouts.value = workouts
            
            // Sort workouts by sequence number for the stack display
            val sortedWorkouts = workouts.sortedBy { it.sequenceNumber }
            _workoutsForActivePlan.value = sortedWorkouts
            
            // Queue-based: simple round-robin through all workouts
            if (workouts.isNotEmpty()) {
                val completedWorkouts = workouts.filter { it.completed }
                val activeWorkouts = workouts.filter { !it.completed }
                
                val nextWorkout = if (activeWorkouts.isNotEmpty()) {
                    // Use a simple rotating queue based on sequence
                    activeWorkouts.minByOrNull { it.sequenceNumber } 
                } else {
                    // All workouts completed, reset and start over
                    workouts.forEach { workout ->
                        repository.updateWorkout(workout.copy(completed = false))
                    }
                    workouts.minByOrNull { it.sequenceNumber }
                }
                
                _nextWorkout.value = nextWorkout
            } else {
                _nextWorkout.value = null
            }
        }
    }

    suspend fun createWorkoutPlan(name: String, description: String, workouts: List<WorkoutWithExercisesData>): Long {
        val plan = WorkoutPlan(
            name = name,
            description = description,
            isActive = true
        )
        
        // Deactivate existing plans
        _workoutPlans.value.forEach { existingPlan ->
            if (existingPlan.isActive) {
                repository.updateWorkoutPlan(existingPlan.copy(isActive = false))
            }
        }
        
        val planId = repository.insertWorkoutPlan(plan)
        
        // Create workouts and exercises
        workouts.forEachIndexed { index, workoutData ->
            val workout = Workout(
                name = workoutData.name,
                category = workoutData.category,
                sequenceNumber = index + 1
            )
            val workoutId = repository.insertWorkout(workout)
            
            workoutData.exercises.forEachIndexed { exerciseIndex, exerciseData ->
                // First, create the exercise if it doesn't exist
                val exercise = Exercise(
                    name = exerciseData.name,
                    category = workoutData.category,
                    muscleGroup = exerciseData.muscleGroup,
                    equipment = "Barbell", // Default, can be updated later
                    description = ""
                )
                val exerciseId = repository.insertExercise(exercise)
                
                // Then create the workout exercise mapping
                val workoutExercise = WorkoutExercise(
                    workoutId = workoutId.toInt(),
                    exerciseId = exerciseId.toInt(),
                    exerciseName = exerciseData.name,
                    orderIndex = exerciseIndex,
                    targetSets = exerciseData.sets,
                    targetReps = "10,9,8,7", // Default rep scheme
                    targetWeight = 50.0, // Default starting weight
                    restTime = 90
                )
                repository.insertWorkoutExercise(workoutExercise)
            }
        }
        
        loadWorkoutPlans()
        loadAllWorkouts() // Refresh workouts after creating plan
        return planId
    }

    fun skipNextWorkout() {
        viewModelScope.launch {
            updateNextWorkout()
        }
    }

    fun refreshData() {
        loadWorkoutPlans()
        loadAllWorkouts()
    }
}

data class WorkoutWithExercisesData(
    val name: String,
    val category: String,
    val exercises: List<ExerciseData>
)

data class ExerciseData(
    val name: String,
    val muscleGroup: String,
    val sets: Int
)

class WorkoutPlanViewModelFactory(
    private val application: LyfterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutPlanViewModel(application.repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
