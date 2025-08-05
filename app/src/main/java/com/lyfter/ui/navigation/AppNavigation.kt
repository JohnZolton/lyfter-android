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
        composable(
            route = "workout/{workoutId}",
            arguments = listOf(
                androidx.navigation.navArgument("workoutId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: 0
            WorkoutScreen(navController, workoutId)
        }
        composable("exercises") {
            ExerciseListScreen(navController)
        }
        composable("create-workout-plan") {
            CreateWorkoutPlanScreen(navController, planId = null)
        }
        composable(
            route = "edit-workout-plan/{planId}",
            arguments = listOf(
                androidx.navigation.navArgument("planId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getInt("planId")
            CreateWorkoutPlanScreen(navController, planId)
        }
    }
}
