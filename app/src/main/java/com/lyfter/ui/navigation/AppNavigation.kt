package com.lyfter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lyfter.ui.screens.HomeScreen
import com.lyfter.ui.screens.WorkoutScreen
import com.lyfter.ui.screens.ExerciseListScreen
import com.lyfter.ui.screens.CreateWorkoutPlanScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("workout/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "push"
            WorkoutScreen(navController, category)
        }
        composable("exercises") {
            ExerciseListScreen(navController)
        }
        composable("create-workout-plan") {
            CreateWorkoutPlanScreen(navController, planId = null)
        }
        composable("edit-workout-plan/{planId}") { backStackEntry ->
            val planId = backStackEntry.arguments?.getString("planId")?.toLongOrNull()
            CreateWorkoutPlanScreen(navController, planId)
        }
    }
}
