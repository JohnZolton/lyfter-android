package com.lyfter.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val showMicroDeloadNotification: StateFlow<Boolean> = _showMicroDeloadNotification.asStateFlow()

    fun loadWorkout(category: String) {
        viewModelScope.launch {
            // Load the latest workout for this category
            val workout = repository.getLatestWorkoutByCategory(category)
            
            workout?.let {
                _currentWorkout.value = it
                loadWorkoutExercises(it.id)
                loadPreSurvey(it.id)
                
                // Check for micro deload on the loaded workout
                val shouldDeload = repository.shouldApplyMicroDeload(category, it.sequenceNumber)
                if (shouldDeload) {
                    _showMicroDeloadNotification.value = true
                    repository.applyMicroDeloadSets(it.id)
                    repository.applyMicroDeload(it.id)
                    loadWorkoutExercises(it.id) // Reload after deload
                }
            }
        }
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
        sequenceNumber: Int,
        setNumber: Int,
        reps: Int,
        weight: Double
    ) {
        viewModelScope.launch {
            val result = repository.compareWithPreviousCycle(
                exerciseId, category, sequenceNumber, setNumber, reps, weight
            )
            val key = "$exerciseId-$setNumber"
            _comparisonResults.value = _comparisonResults.value + (key to result)
        }
    }

    fun dismissMicroDeloadNotification() {
        _showMicroDeloadNotification.value = false
    }

    fun addSet(workoutExerciseId: Int, currentSets: List<ExerciseSet>) {
        viewModelScope.launch {
            val nextSetNumber = (currentSets.maxOfOrNull { it.setNumber } ?: 0) + 1
            val lastSet = currentSets.maxByOrNull { it.setNumber }
            
            // Get targets for this exercise
            val targets = getTargetValues(workoutExerciseId)
            
            val newSet = ExerciseSet(
                workoutExerciseId = workoutExerciseId,
                setNumber = nextSetNumber,
                reps = 0,
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
        if (currentWorkout.sequenceNumber <= 1) {
            return TargetValues(weight = 50.0, reps = 10)
        }
        
        workoutExercise?.let { exercise ->
            val previousWorkoutId = findPreviousWorkoutId(exercise.exerciseId, currentWorkout.category, currentWorkout.sequenceNumber)
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

    private suspend fun findPreviousWorkoutId(exerciseId: Int, category: String, currentSequence: Int): Int? {
        val previousSequence = currentSequence - 1
        if (previousSequence < 1) return null
        
        val previousWorkout = repository.getWorkoutByCategoryAndSequence(category, previousSequence)
        return previousWorkout?.id
    }

    suspend fun getPostExerciseSurveyForExercise(workoutExerciseId: Int): PostExerciseSurvey? {
        return repository.getPostExerciseSurvey(workoutExerciseId)
    }

    suspend fun getTargetValuesForSet(workoutExerciseId: Int, setNumber: Int): TargetValues {
        return getTargetValues(workoutExerciseId) 
    }

    suspend fun getWorkoutById(workoutId: Int): Workout? {
        return repository.getWorkoutById(workoutId)
    }

    suspend fun shouldApplyMicroDeload(category: String, sequenceNumber: Int): Boolean {
        return repository.shouldApplyMicroDeload(category, sequenceNumber)
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
