package com.lyfter.repository

import com.lyfter.data.AppDatabase
import com.lyfter.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

class WorkoutRepository(
    private val database: AppDatabase
) {
    // Exercise operations
    suspend fun insertExercise(exercise: Exercise): Long {
        return database.exerciseDao().insert(exercise)
    }

    fun getAllExercises(): Flow<List<Exercise>> {
        return database.exerciseDao().getAll()
    }

    fun getExercisesByCategory(category: String): Flow<List<Exercise>> {
        return database.exerciseDao().getByCategory(category)
    }

    // Workout operations
    suspend fun insertWorkout(workout: Workout): Long {
        return database.workoutDao().insert(workout)
    }

    suspend fun updateWorkout(workout: Workout) {
        database.workoutDao().update(workout)
    }

    suspend fun getWorkoutById(id: Int): Workout? {
        return database.workoutDao().getById(id)
    }

    fun getAllWorkouts(): Flow<List<Workout>> {
        return database.workoutDao().getAll()
    }

    suspend fun getLatestWorkoutByCategory(category: String): Workout? {
        return database.workoutDao().getLatestByCategory(category)
    }

    suspend fun getWorkoutByCategoryAndCycle(category: String, cycleNumber: Int): Workout? {
        return database.workoutDao().getByCategoryAndCycle(category, cycleNumber)
    }

    suspend fun shouldApplyMicroDeload(category: String, cycleNumber: Int): Boolean {
        // Apply micro deload every 3rd workout in a cycle
        return cycleNumber % 3 == 0
    }


    // WorkoutExercise operations
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long {
        return database.workoutExerciseDao().insert(workoutExercise)
    }

    fun getWorkoutExercises(workoutId: Int): Flow<List<WorkoutExercise>> {
        return database.workoutExerciseDao().getByWorkoutId(workoutId)
    }

    // Set operations
    suspend fun insertSet(set: ExerciseSet): Long {
        return database.setDao().insert(set)
    }

    suspend fun updateSet(set: ExerciseSet) {
        database.setDao().update(set)
    }

    suspend fun deleteSet(set: ExerciseSet) {
        database.setDao().delete(set)
    }

    suspend fun deleteSetFromWorkoutExercise(workoutExerciseId: Int, setNumber: Int) {
        database.setDao().deleteSetFromWorkoutExercise(workoutExerciseId, setNumber)
    }

    suspend fun getSetsForWorkoutExerciseSync(workoutExerciseId: Int): List<ExerciseSet> {
        return database.setDao().getByWorkoutExerciseIdSync(workoutExerciseId)
    }

    fun getSetsForWorkoutExerciseFlow(workoutExerciseId: Int): Flow<List<ExerciseSet>> {
        return database.setDao().getByWorkoutExerciseId(workoutExerciseId)
    }

    suspend fun getAllSetsForWorkout(workoutId: Int): List<ExerciseSet> {
        return database.setDao().getAllSetsForWorkout(workoutId)
    }

    // WorkoutPlan operations
    suspend fun insertWorkoutPlan(workoutPlan: WorkoutPlan): Long {
        return database.workoutPlanDao().insert(workoutPlan)
    }

    suspend fun updateWorkoutPlan(workoutPlan: WorkoutPlan) {
        database.workoutPlanDao().update(workoutPlan)
    }

    suspend fun deleteWorkoutPlan(workoutPlan: WorkoutPlan) {
        database.workoutPlanDao().delete(workoutPlan)
    }

    suspend fun getWorkoutPlanById(id: Int): WorkoutPlan? {
        return database.workoutPlanDao().getById(id)
    }

    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>> {
        return database.workoutPlanDao().getAll()
    }

    fun getActiveWorkoutPlans(): Flow<List<WorkoutPlan>> {
        return database.workoutPlanDao().getActivePlans()
    }

    // Survey operations
    suspend fun savePreExerciseSurvey(survey: PreExerciseSurvey): Long {
        return database.surveyDao().insertPreExerciseSurvey(survey)
    }

    suspend fun getPreExerciseSurvey(workoutId: Int): PreExerciseSurvey? {
        return database.surveyDao().getPreExerciseSurvey(workoutId)
    }

    suspend fun savePostExerciseSurvey(survey: PostExerciseSurvey): Long {
        return database.surveyDao().insertPostExerciseSurvey(survey)
    }

    suspend fun getPostExerciseSurvey(workoutExerciseId: Int): PostExerciseSurvey? {
        return database.surveyDao().getPostExerciseSurvey(workoutExerciseId)
    }

    suspend fun getPostExerciseSurveysForWorkout(workoutId: Int): List<PostExerciseSurvey> {
        return database.surveyDao().getPostExerciseSurveysForWorkout(workoutId)
    }

    // Exercise completion operations
    suspend fun markExerciseCompleted(workoutExerciseId: Int) {
        database.workoutExerciseDao().updateExerciseCompletion(workoutExerciseId, true, Date())
    }

    suspend fun markExerciseIncomplete(workoutExerciseId: Int) {
        database.workoutExerciseDao().updateExerciseCompletion(workoutExerciseId, false, null)
    }

    suspend fun getNextExerciseToSurvey(workoutId: Int): WorkoutExercise? {
        return database.workoutExerciseDao().getNextUncompletedExercise(workoutId)
    }

    // Adjustment logic based on surveys
    suspend fun adjustSetsBasedOnRecovery(workoutId: Int, recoveryStatus: RecoveryStatus) {
        val workout = database.workoutDao().getById(workoutId) ?: return
        val exercises = database.workoutExerciseDao().getByWorkoutIdSync(workoutId)
        
        val setAdjustment = when (recoveryStatus) {
            RecoveryStatus.A_WHILE_AGO -> 1      // Add 1 set
            RecoveryStatus.JUST_IN_TIME -> 0     // Keep same
            RecoveryStatus.STILL_SORE -> -1      // Remove 1 set
            RecoveryStatus.STILL_REALLY_SORE -> -2 // Remove 2 sets
        }
        
        exercises.forEach { exercise ->
            adjustExerciseSets(exercise.id, setAdjustment)
        }
    }

    suspend fun adjustSetsBasedOnFeedback(workoutExerciseId: Int, intensity: WorkoutIntensity, pumpRating: PumpRating) {
        // Add sets if not challenging enough and pump is low/medium
        val shouldIncrease = when {
            intensity == WorkoutIntensity.EASY && pumpRating == PumpRating.LOW -> true
            intensity == WorkoutIntensity.EASY && pumpRating == PumpRating.MEDIUM -> true
            intensity == WorkoutIntensity.MODERATE && pumpRating == PumpRating.LOW -> true
            else -> false
        }
        
        // Remove sets if too challenging
        val shouldDecrease = when {
            intensity == WorkoutIntensity.PUSHED_LIMITS -> true
            else -> false
        }
        
        val setAdjustment = when {
            shouldIncrease -> 1
            shouldDecrease -> -1
            else -> 0
        }
        
        if (setAdjustment != 0) {
            adjustExerciseSets(workoutExerciseId, setAdjustment)
        }
    }

    private suspend fun adjustExerciseSets(workoutExerciseId: Int, adjustment: Int) {
        val currentSets = database.setDao().getByWorkoutExerciseIdSync(workoutExerciseId)
        val newTargetCount = currentSets.size + adjustment
        
        // Ensure minimum 2 sets, maximum 6 sets
        val targetCount = when {
            newTargetCount < 2 -> 2
            newTargetCount > 6 -> 6
            else -> newTargetCount
        }
        
        // Adjust sets to match target count
        if (currentSets.size < targetCount) {
            // Add sets
            val lastSetNumber = currentSets.maxOfOrNull { it.setNumber } ?: 0
            for (i in 1..(targetCount - currentSets.size)) {
                val newSet = ExerciseSet(
                    workoutExerciseId = workoutExerciseId,
                    setNumber = lastSetNumber + i,
                    reps = currentSets.maxOfOrNull { it.reps } ?: 10,
                    weight = currentSets.maxOfOrNull { it.weight } ?: 50.0
                )
                database.setDao().insert(newSet)
            }
        } else if (currentSets.size > targetCount) {
            // Remove excess sets (highest set numbers first)
            val setsToRemove = currentSets.sortedBy { it.setNumber }.takeLast(currentSets.size - targetCount)
            setsToRemove.forEach { set ->
                database.setDao().delete(set)
            }
        }
    }


    suspend fun applyMicroDeload(workoutId: Int) {
        val exercises = database.workoutExerciseDao().getByWorkoutIdSync(workoutId)
        
        exercises.forEach { exercise ->
            val sets = database.setDao().getByWorkoutExerciseIdSync(exercise.id)
            sets.forEach { set ->
                val deloadSet = set.copy(
                    weight = set.weight * 0.75, // 75% of previous weight
                    // Sets remain at 50% of current count, handled by calling method
                )
                database.setDao().update(deloadSet)
            }
        }
    }

    suspend fun applyMicroDeloadSets(workoutId: Int) {
        val exercises = database.workoutExerciseDao().getByWorkoutIdSync(workoutId)
        
        exercises.forEach { exercise ->
            val sets = database.setDao().getByWorkoutExerciseIdSync(exercise.id)
            val targetSetCount = (sets.size / 2).coerceAtLeast(2) // Ensure minimum 2 sets
            
            // Adjust to half sets
            if (sets.size > targetSetCount) {
                val setsToDelete = sets.sortedBy { it.setNumber }.takeLast(sets.size - targetSetCount)
                setsToDelete.forEach { set ->
                    database.setDao().delete(set)
                }
            }
            
            // Apply 75% weight reduction to remaining sets
            sets.take(targetSetCount).forEach { set ->
                val deloadSet = set.copy(weight = set.weight * 0.75)
                database.setDao().update(deloadSet)
            }
        }
    }

    suspend fun getWorkoutExerciseById(id: Int): WorkoutExercise? {
        return database.workoutExerciseDao().getById(id)
    }

    suspend fun getWorkoutExercisesByWorkoutIdSync(workoutId: Int): List<WorkoutExercise> {
        return database.workoutExerciseDao().getByWorkoutIdSync(workoutId)
    }

    suspend fun getWorkoutExerciseByWorkoutAndExercise(workoutId: Int, exerciseId: Int): WorkoutExercise? {
        return database.workoutExerciseDao().getByWorkoutAndExercise(workoutId, exerciseId)
    }

    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise) {
        database.workoutExerciseDao().update(workoutExercise)
    }

    suspend fun moveExerciseUp(workoutExerciseId: Int) {
        val exercise = getWorkoutExerciseById(workoutExerciseId) ?: return
        val exercises = getWorkoutExercisesByWorkoutIdSync(exercise.workoutId)
            .sortedBy { it.orderIndex }
        
        val currentIndex = exercises.indexOfFirst { it.id == workoutExerciseId }
        if (currentIndex > 0) {
            val currentExercise = exercises[currentIndex]
            val previousExercise = exercises[currentIndex - 1]
            
            // Swap order indices
            database.workoutExerciseDao().updateOrderIndex(currentExercise.id, previousExercise.orderIndex)
            database.workoutExerciseDao().updateOrderIndex(previousExercise.id, currentExercise.orderIndex)
        }
    }

    suspend fun moveExerciseDown(workoutExerciseId: Int) {
        val exercise = getWorkoutExerciseById(workoutExerciseId) ?: return
        val exercises = getWorkoutExercisesByWorkoutIdSync(exercise.workoutId)
            .sortedBy { it.orderIndex }
        
        val currentIndex = exercises.indexOfFirst { it.id == workoutExerciseId }
        if (currentIndex < exercises.size - 1) {
            val currentExercise = exercises[currentIndex]
            val nextExercise = exercises[currentIndex + 1]
            
            // Swap order indices
            database.workoutExerciseDao().updateOrderIndex(currentExercise.id, nextExercise.orderIndex)
            database.workoutExerciseDao().updateOrderIndex(nextExercise.id, currentExercise.orderIndex)
        }
    }

    // Comparison logic for status indicators
    suspend fun getPreviousCycleSet(
        exerciseId: Int,
        category: String,
        currentCycleNumber: Int,
        setNumber: Int
    ): ExerciseSet? {
        val previousCycleNumber = currentCycleNumber - 1
        if (previousCycleNumber < 1) return null

        // Get the previous workout by category and cycle number
        val previousWorkout = database.workoutDao().getByCategoryAndCycle(category, previousCycleNumber) ?: return null
        
        val previousWorkoutExercise = database.workoutExerciseDao()
            .getByWorkoutAndExercise(previousWorkout.id, exerciseId) ?: return null
        
        return database.setDao()
            .getByWorkoutExerciseAndSetNumber(previousWorkoutExercise.id, setNumber)
    }

    suspend fun compareWithPreviousCycle(
        exerciseId: Int,
        category: String,
        currentCycleNumber: Int,
        setNumber: Int,
        currentReps: Int,
        currentWeight: Double
    ): ComparisonResult {
        val previousSet = getPreviousCycleSet(
            exerciseId, category, currentCycleNumber, setNumber
        ) ?: return ComparisonResult.NO_PREVIOUS

        val repsImproved = currentReps > previousSet.reps
        val weightImproved = currentWeight > previousSet.weight
        
        return when {
            repsImproved || weightImproved -> ComparisonResult.IMPROVED
            currentReps == previousSet.reps && currentWeight == previousSet.weight -> ComparisonResult.SAME
            else -> ComparisonResult.REGRESSED
        }
    }
}

enum class ComparisonResult {
    NO_PREVIOUS,
    IMPROVED,
    SAME,
    REGRESSED
}
