package com.lyfter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyfter.LyfterApplication
import com.lyfter.model.ExerciseSet
import com.lyfter.model.Workout
import com.lyfter.model.WorkoutExercise
import com.lyfter.model.PreExerciseSurvey
import com.lyfter.model.PostExerciseSurvey
import com.lyfter.model.RecoveryStatus
import com.lyfter.model.WorkoutIntensity
import com.lyfter.model.PumpRating
import com.lyfter.repository.WorkoutRepository
import com.lyfter.repository.ComparisonResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    private val _currentWorkout = MutableStateFlow<Workout?>(null)
    val currentWorkout: StateFlow<Workout?> = _currentWorkout.asStateFlow()
    
    private val _workoutExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val workoutExercises: StateFlow<List<WorkoutExercise>> = _workoutExercises.asStateFlow()
    
    private val _sets = MutableStateFlow<List<ExerciseSet>>(emptyList())
    val sets: StateFlow<List<ExerciseSet>> = _sets.asStateFlow()
    
    private val _comparisonResults = MutableStateFlow<Map<String, ComparisonResult>>(emptyMap())
    val comparisonResults: StateFlow<Map<String, ComparisonResult>> = _comparisonResults.asStateFlow()

    private val _preExerciseSurvey = MutableStateFlow<PreExerciseSurvey?>(null)
    val preExerciseSurvey: StateFlow<PreExerciseSurvey?> = _preExerciseSurvey.asStateFlow()

    private val _showMicroDeloadNotification = MutableStateFlow(false)
    val showSanctionNotification: StateFlow<Boolean> = _showMicroDeloadNotification.asStateFlow()

    fun loadWorkout(category: String) {
        viewModelScope.launch {
            // Load the latest workout for this category
            val workout = repository.getLatestWorkoutByCategory(category)
            
            workout?.let { 
                _currentWorkout.value = it
                loadWorkoutExercises(it.id)
                loadPreSurvey(it.id)
                
                // Check for micro deload on the loaded workout
                val shouldDeload = repository.shouldApplyMicroDeload(category, it.cycleNumber)
                if (shouldDeload) {
                    _showMicroDeloadNotification.value = true
                    repository.applyMicroDeloadSets(it.id)
                    repository.applyMicroDeload(it.id)
                    loadWorkoutExercises(it.id) // Reload after deload
                }
            }
        }
    }

    fun loadWorkoutById(workoutId: Int) {
        viewModelScope.launch {
            // Load the specific workout by ID
            val workoutFetched = repository.getWorkoutById(workoutId)
            
            workoutFetched?.let { workout ->
                _currentWorkout.value = workout
                loadWorkoutExercises(workout.id)
                loadPreSurvey(workout.id)
                
                // Check for micro deload on the loaded workout
                val shouldDeload = repository.shouldApplyMicroDeload(workout.category, workout.cycleNumber)
                if (shouldDeload) {
                    _showMicroDeloadNotification.value = true
                    repository.applyMicroDeloadSets(workout.id)
                    repository.applyMicroDeload(workout.id)
                    loadWorkoutExercises(workout.id) // Reload after deload
                } else {
                    // Only update exercise targets if not applying micro deload
                    updateExerciseTargetsForCycle(workout)
                    // Reload exercises after updating targets
                    loadWorkoutExercises(workout.id)
                }
            }
        }
    }
    
    suspend fun shouldShowPreSurvey(workoutId: Int): Boolean {
        val workout = repository.getWorkoutById(workoutId)
        return workout?.cycleNumber ?: 0 > 1
    }
    
    private fun loadWorkoutExercises(workoutId: Int) {
        viewModelScope.launch {
            repository.getWorkoutExercises(workoutId).collect { exercises ->
                _workoutExercises.value = exercises
                exercises.forEach { exercise ->
                    loadSets(exercise.id)
                }
            }
        }
    }
    
    private fun loadSets(workoutExerciseId: Int) {
        viewModelScope.launch {
            repository.getSetsForWorkoutExerciseFlow(workoutExerciseId).collect { sets ->
                _sets.value = _sets.value.filter { it.workoutExerciseId != workoutExerciseId } + sets
            }
        }
    }
    
    private fun loadPreSurvey(workoutId: Int) {
        viewModelScope.launch {
            _preExerciseSurvey.value = repository.getPreExerciseSurvey(workoutId)
        }
    }
    
    fun savePreExerciseSurvey(recoveryStatus: RecoveryStatus) {
        viewModelScope.launch {
            _currentWorkout.value?.let { workout ->
                val survey = PreExerciseSurvey(
                    workoutId = workout.id,
                    recoveryStatus = recoveryStatus
                )
                repository.savePreExerciseSurvey(survey)
                _preExerciseSurvey.value = survey
                
                // Adjust sets based on recovery status
                repository.adjustSetsBasedOnRecovery(workout.id, recoveryStatus)
                loadWorkoutExercises(workout.id) // Refresh workout
            }
        }
    }

    fun savePostExerciseSurvey(workoutExerciseId: Int, intensity: WorkoutIntensity, pumpRating: PumpRating) {
        viewModelScope.launch {
            val survey = PostExerciseSurvey(
                workoutExerciseId = workoutExerciseId,
                intensity = intensity,
                pumpRating = pumpRating
            )
            repository.savePostExerciseSurvey(survey)
            
            // Adjust sets based on per-exercise feedback
            repository.adjustSetsBasedOnFeedback(workoutExerciseId, intensity, pumpRating)
            
            // Mark exercise as completed
            repository.markExerciseCompleted(workoutExerciseId)
            loadWorkoutExercises(_currentWorkout.value?.id ?: return@launch)
        }
    }

    fun markExerciseCompleted(workoutExerciseId: Int) {
        viewModelScope.launch {
            repository.markExerciseCompleted(workoutExerciseId)
            loadWorkoutExercises(_currentWorkout.value?.id ?: return@launch)
        }
    }
    
    fun updateSet(workoutExerciseId: Int, setNumber: Int, reps: Int, weight: Double) {
        viewModelScope.launch {
            val existingSet = _sets.value.find { 
                it.workoutExerciseId == workoutExerciseId && it.setNumber == setNumber 
            }
            
            if (existingSet != null) {
                val updatedSet = existingSet.copy(reps = reps, weight = weight)
                repository.updateSet(updatedSet)
                
                // Cascade weight down to following sets
                val setsForExercise = _sets.value.filter { it.workoutExerciseId == workoutExerciseId }
                val followingSets = setsForExercise.filter { it.setNumber > setNumber }
                
                val updatedSets = followingSets.map { set ->
                    set.copy(weight = weight)
                }
                
                updatedSets.forEach { set ->
                    repository.updateSet(set)
                }
                
                // Update UI state to reflect all changes
                val allOtherSets = _sets.value.filter { 
                    it.workoutExerciseId != workoutExerciseId || 
                    (it.workoutExerciseId == workoutExerciseId && it.setNumber != setNumber) 
                }
                
                _sets.value = allOtherSets + updatedSet + updatedSets
            } else {
                val newSet = ExerciseSet(
                    workoutExerciseId = workoutExerciseId,
                    setNumber = setNumber,
                    reps = reps,
                    weight = weight
                )
                repository.insertSet(newSet)
            }
        }
    }
    
    fun compareSet(
        exerciseId: Int,
        category: String,
        cycleNumber: Int,
        setNumber: Int,
        reps: Int,
        weight: Double
    ) {
        viewModelScope.launch {
            val result = repository.compareWithPreviousCycle(
                exerciseId, category, cycleNumber, setNumber, reps, weight
            )
            val key = "$exerciseId-$setNumber"
            _comparisonResults.value = _comparisonResults.value + Pair(key, result)
        }
    }

    fun dismissMicroDeloadNotificationSanction() {
        _showMicroDeloadNotification.value = false
    }

    fun addSet(workoutExerciseId: Int, currentSets: List<ExerciseSet>) {
        viewModelScope.launch {
            val nextSetNumber = (currentSets.maxOfOrNull { it.setNumber } ?: 0) + 1
            
            // Get targets for this exercise
            val targets = getTargetValues(workoutExerciseId)
            
            val newSet = ExerciseSet(
                workoutExerciseId = workoutExerciseId,
                setNumber = nextSetNumber,
                reps = 0,  // Keep reps empty when adding a new set
                weight = targets.weight
            )
            
            repository.insertSet(newSet)
            loadSets(workoutExerciseId)
        }
    }

    fun removeSet(workoutExerciseId: Int, currentSets: List<ExerciseSet>) {
        viewModelScope.launch {
            if (currentSets.isNotEmpty()) {
                val lastSet = currentSets.maxByOrNull { it.setNumber }
                lastSet?.let {
                    repository.deleteSetFromWorkoutExercise(workoutExerciseId, it.setNumber)
                    loadSets(workoutExerciseId)
                }
            }
        }
    }

    private suspend fun getTargetValues(workoutExerciseId: Int): TargetValues {
        val workoutExercise = repository.getWorkoutExerciseById(workoutExerciseId)
        val currentWorkout = _currentWorkout.value ?: return TargetValues(0.0, 0)
        
        // For first workout cycle, use default values instead of looking at previous cycles
        if (currentWorkout.cycleNumber <= 1) {
            return TargetValues(weight = 50.0, reps = 10)
        }
        
        workoutExercise?.let { exercise ->
            val previousWorkoutId = findPreviousWorkoutId(exercise.exerciseId, currentWorkout.category, currentWorkout.cycleNumber)
            if (previousWorkoutId != null) {
                val previousWorkoutExercise = repository.getWorkoutExerciseByWorkoutAndExercise(previousWorkoutId, exercise.exerciseId)
                if (previousWorkoutExercise != null) {
                    val lastSets = repository.getSetsForWorkoutExerciseSync(previousWorkoutExercise.id)
                    val lastSet = lastSets.maxByOrNull { it.setNumber }
                    
                    if (lastSet != null) {
                        return TargetValues(
                            weight = lastSet.weight + 5.0,
                            reps = lastSet.reps + 1
                        )
                    }
                }
            }
        }
        
        return TargetValues(weight = 50.0, reps = 10)
    }

    private suspend fun findPreviousWorkoutId(exerciseId: Int, category: String, currentCycleNumber: Int): Int? {
        val previousCycleNumber = currentCycleNumber - 1
        if (previousCycleNumber < 1) return null
        
        val previousWorkout = repository.getWorkoutByCategoryAndCycle(category, previousCycleNumber)
        return previousWorkout?.id
    }

    suspend fun getPostExerciseSurveyForExercise(workoutExerciseId: Int): PostExerciseSurvey? {
        return repository.getPostExerciseSurvey(workoutExerciseId)
    }

    suspend fun getTargetValuesForSet(workoutExerciseId: Int, setNumber: Int): TargetValues {
        return getTargetValues(workoutExerciseId) 
    }

    suspend fun getSuggestedWeightForSet(
        exerciseId: Int,
        category: String,
        cycleNumber: Int,
        setNumber: Int
    ): Double {
        // For first workout cycle, use default
        if (cycleNumber <= 1) {
            return 50.0
        }
        
        // Get previous workout
        val previousCycle = cycleNumber - 1
        val previousWorkout = repository.getWorkoutByCategoryAndCycle(category, previousCycle)
            ?: return 50.0
        
        // Get previous workout exercise
        val previousWorkoutExercise = repository.getWorkoutExerciseByWorkoutAndExercise(
            previousWorkout.id, 
            exerciseId
        ) ?: return 50.0
        
        // Get the last set from previous workout
        val lastSets = repository.getSetsForWorkoutExerciseSync(previousWorkoutExercise.id)
        val lastSet = lastSets.find { it.setNumber == setNumber }
            ?: lastSets.maxByOrNull { it.setNumber }
        
        return (lastSet?.weight ?: 50.0) + 2.5
    }

    suspend fun getPreviousCycleTargets(
        exerciseId: Int,
        category: String,
        currentCycleNumber: Int,
        setNumber: Int
    ): Pair<Int, Double> {
        // For first workout cycle, return default values
        if (currentCycleNumber <= 1) {
            return Pair(10, 50.0)
        }
        
        // Get previous workout
        val previousCycle = currentCycleNumber - 1
        val previousWorkout = repository.getWorkoutByCategoryAndCycle(category, previousCycle)
            ?: return Pair(10, 50.0)
        
        // Get previous workout exercise
        val previousWorkoutExercise = repository.getWorkoutExerciseByWorkoutAndExercise(
            previousWorkout.id, 
            exerciseId
        ) ?: return Pair(10, 50.0)
        
        // Get the actual set from previous workout
        val lastSets = repository.getSetsForWorkoutExerciseSync(previousWorkoutExercise.id)
        val lastSet = lastSets.find { it.setNumber == setNumber }
            ?: lastSets.maxByOrNull { it.setNumber }
        
        if (lastSet != null) {
            // Use previous cycle's actual performance to determine new rep target
            val previousReps = lastSet.reps
            val previousWeight = lastSet.weight
            
            // Calculate new rep target based on weight comparison
            val newReps = if (previousWeight >= 50.0) { // Assuming 50.0 is the base weight
                if (previousWeight == 50.0) {
                    previousReps + 1
                } else {
                    previousReps
                }
            } else {
                previousReps + 1
            }
            
            // Weight target remains the same as previous performance
            val newWeight = previousWeight
            return Pair(newReps, newWeight)
        }
        
        // Fallback to original targets if no previous data
        val previousExercise = repository.getWorkoutExerciseByWorkoutAndExercise(
            previousWorkout.id, 
            exerciseId
        )
        
        return Pair(
            previousExercise?.targetReps?.split(",")?.first()?.toIntOrNull() ?: 10,
            previousExercise?.targetWeight ?: 50.0
        )
    }

    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return repository.getWorkoutById(workoutId)
    }

    suspend fun shouldApplyMicroDeload(category: String, cycleNumber: Int): Boolean {
        return repository.shouldApplyMicroDeload(category, cycleNumber)
    }

    fun completeWorkoutIfAllExercisesDone(workoutId: Int) {
        viewModelScope.launch {
            val allExercises = repository.getWorkoutExercisesByWorkoutIdSync(workoutId)
            val allCompleted = allExercises.all { it.completed }
            
            if (allCompleted && allExercises.isNotEmpty()) {
                val workout = repository.getWorkoutById(workoutId) ?: return@launch
                val updatedWorkout = workout.copy(completed = true)
                repository.updateWorkout(updatedWorkout)
            }
        }
    }

    fun moveExerciseUp(workoutExerciseId: Int) {
        viewModelScope.launch {
            repository.moveExerciseUp(workoutExerciseId)
            _currentWorkout.value?.let { loadWorkoutExercises(it.id) }
        }
    }

    fun moveExerciseDown(workoutExerciseId: Int) {
        viewModelScope.launch {
            repository.moveExerciseDown(workoutExerciseId)
            _currentWorkout.value?.let { loadWorkoutExercises(it.id) }
        }
    }

    fun exitWorkout(workoutId: Int) {
        viewModelScope.launch {
            // Mark workout as completed if all exercises are done
            val allExercises = repository.getWorkoutExercisesByWorkoutIdSync(workoutId)
            val allCompleted = allExercises.all { it.completed }
            
            if (allCompleted && allExercises.isNotEmpty()) {
                val workout = repository.getWorkoutById(workoutId) ?: return@launch
                val updatedWorkout = workout.copy(completed = true)
                repository.updateWorkout(updatedWorkout)
            }
        }
    }

    private suspend fun updateExerciseTargetsForCycle(workout: Workout) {
        if (workout.cycleNumber <= 1) return
        
        val exercises = repository.getWorkoutExercisesByWorkoutIdSync(workout.id)
        
        exercises.forEach { exercise ->
            // Get previous workout
            val previousWorkout = repository.getWorkoutByCategoryAndCycle(
                workout.category, 
                workout.cycleNumber - 1
            ) ?: return@forEach
            
            val previousExercise = repository.getWorkoutExerciseByWorkoutAndExercise(
                previousWorkout.id,
                exercise.exerciseId
            ) ?: return@forEach
            
            // Get the actual sets from previous workout
            val previousSets = repository.getSetsForWorkoutExerciseSync(previousExercise.id)
            if (previousSets.isEmpty()) return@forEach
            
            // Calculate new targets based on previous performance
            val lastSet = previousSets.maxByOrNull { it.setNumber }
            if (lastSet != null) {
                val previousReps = lastSet.reps
                val previousWeight = lastSet.weight
                
                // Calculate new rep target based on weight comparison
                val newTargetReps = if (previousWeight >= 50.0) { // Assuming 50.0 is the base weight
                    if (previousWeight == 50.0) {
                        previousReps + 1
                    } else {
                        previousReps
                    }
                } else {
                    previousReps + 1
                }
                
                // Weight target remains the same as previous performance
                val newTargetWeight = previousWeight
                
                // Update the exercise with new targets
                val updatedExercise = exercise.copy(
                    targetReps = newTargetReps.toString(),
                    targetWeight = newTargetWeight
                )
                repository.updateWorkoutExercise(updatedExercise)
            }
        }
    }
}

data class TargetValues(
    val weight: Double,
    val reps: Int
)

class WorkoutViewModelFactory(
    private val application: LyfterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(application.repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
