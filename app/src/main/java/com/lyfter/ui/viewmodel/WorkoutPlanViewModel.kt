package com.lyfter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyfter.LyfterApplication
import com.lyfter.model.WorkoutPlan
import com.lyfter.model.Workout
import com.lyfter.model.Exercise
import com.lyfter.model.WorkoutExercise
import com.lyfter.model.ExerciseSet
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
            
            // Sort workouts by cycle number for the stack display
            val sortedWorkouts = workouts.sortedBy { it.cycleNumber }
            _workoutsForActivePlan.value = sortedWorkouts
            
            // Cycle-based: group workouts by category and find next in cycle
            if (workouts.isNotEmpty()) {
                val nextWorkout = findNextWorkoutInCycle(workouts)
                _nextWorkout.value = nextWorkout
            } else {
                _nextWorkout.value = null
            }
        }
    }
    
    private fun findNextWorkoutInCycle(workouts: List<Workout>): Workout? {
        // Sort all workouts by cycle number
        val sortedWorkouts = workouts.sortedBy { it.cycleNumber }
        
        // Find incomplete workouts
        val incompleteWorkouts = sortedWorkouts.filter { !it.completed }
        
        if (incompleteWorkouts.isNotEmpty()) {
            // Find the workout with the lowest cycle number among incomplete workouts
            return incompleteWorkouts.minByOrNull { it.cycleNumber }
        }
        
        // If all workouts are completed, create new cycle
        // We can't directly return the result of a suspend function, so we'll handle it asynchronously
        viewModelScope.launch {
            val nextWorkout = createNextWorkoutCycle(workouts)
            _nextWorkout.value = nextWorkout
        }
        return null
    }

    private suspend fun createNextWorkoutCycle(previousWorkouts: List<Workout>): Workout? {
        if (previousWorkouts.isEmpty()) return null
        
        // Group workouts by cycle number to understand the structure
        val workoutsByCycle = previousWorkouts.groupBy { it.cycleNumber }
        val maxCycle = workoutsByCycle.keys.maxOrNull() ?: 0
        
        // Check if all workouts in the current cycle are completed
        val currentCycleWorkouts = workoutsByCycle[maxCycle] ?: emptyList()
        val allCurrentCycleCompleted = currentCycleWorkouts.isNotEmpty() && currentCycleWorkouts.all { it.completed }
        
        // If all workouts in the current cycle are completed, increment cycle number
        // Otherwise, use the same cycle number
        val nextCycleNumber = if (allCurrentCycleCompleted) {
            maxCycle + 1
        } else {
            maxCycle
        }
        
        // Find the workout that should be next in the cycle (highest cycle in previous cycle)
        val lastWorkoutInCycle = previousWorkouts.maxByOrNull { it.cycleNumber } ?: return null
        
        // Create new workout for next cycle
        val newWorkout = Workout(
            name = lastWorkoutInCycle.name,
            category = lastWorkoutInCycle.category,
            cycleNumber = nextCycleNumber,
            completed = false
        )
        
        val newWorkoutId = repository.insertWorkout(newWorkout)
        
        // Copy exercises from the previous cycle's workout and update targets based on performance
        val previousExercises = repository.getWorkoutExercisesByWorkoutIdSync(lastWorkoutInCycle.id)
        
        previousExercises.forEach { prevExercise ->
            // Create new workout exercise
            val newWorkoutExercise = WorkoutExercise(
                workoutId = newWorkoutId.toInt(),
                exerciseId = prevExercise.exerciseId,
                exerciseName = prevExercise.exerciseName,
                orderIndex = prevExercise.orderIndex,
                targetSets = prevExercise.targetSets,
                targetReps = prevExercise.targetReps,
                targetWeight = prevExercise.targetWeight,
                restTime = prevExercise.restTime
            )
            
            val newWorkoutExerciseId = repository.insertWorkoutExercise(newWorkoutExercise)
            
            // Get last performance to set new targets
            val lastSets = repository.getSetsForWorkoutExerciseSync(prevExercise.id)
            if (lastSets.isNotEmpty()) {
                val lastSet = lastSets.maxByOrNull { it.setNumber }
                lastSet?.let { set ->
                    // Increase weight by 2.5 lbs and reps by 1 for progressive overload
                    val newWeight = set.weight + 2.5
                    val newReps = set.reps + 1
                    
                    // Update the new workout exercise with improved targets
                    val updatedExercise = newWorkoutExercise.copy(
                        targetWeight = newWeight,
                        targetReps = "$newReps"
                    )
                    repository.updateWorkoutExercise(updatedExercise)
                    
                    // Create sets for the new workout exercise with updated targets
                    for (i in 1..updatedExercise.targetSets) {
                        val newSet = ExerciseSet(
                            workoutExerciseId = newWorkoutExerciseId.toInt(),
                            setNumber = i,
                            reps = 0, // Leave empty for user input
                            weight = newWeight // Use target weight for new sets
                        )
                        repository.insertSet(newSet)
                    }
                }
            } else {
                // No previous performance, use original targets
                for (i in 1..newWorkoutExercise.targetSets) {
                    val newSet = ExerciseSet(
                        workoutExerciseId = newWorkoutExerciseId.toInt(),
                        setNumber = i,
                        reps = 0, // Leave empty for user input
                        weight = 50.0 // Default
                    )
                    repository.insertSet(newSet)
                }
            }
        }
        
        return newWorkout
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
                cycleNumber = index + 1
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
                    targetReps = "", // No targets for first round
                    targetWeight = 0.0, // No targets for first round
                    restTime = 90
                )
                val workoutExerciseId = repository.insertWorkoutExercise(workoutExercise)
                
                // Create sets for the workout exercise
                for (i in 1..exerciseData.sets) {
                    val newSet = ExerciseSet(
                        workoutExerciseId = workoutExerciseId.toInt(),
                        setNumber = i,
                        reps = 0, // Empty for first round
                        weight = 0.0 // Empty for first round
                    )
                    repository.insertSet(newSet)
                }
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
