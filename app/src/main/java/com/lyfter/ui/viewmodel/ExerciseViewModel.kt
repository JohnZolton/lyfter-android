package com.lyfter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyfter.LyfterApplication
import com.lyfter.model.Exercise
import com.lyfter.repository.WorkoutRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    val allExercises: StateFlow<List<Exercise>> = repository.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun insertExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.insertExercise(exercise)
        }
    }
}

class ExerciseViewModelFactory(
    private val application: LyfterApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseViewModel(application.repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
