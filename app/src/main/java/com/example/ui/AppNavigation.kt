package com.example.hyperlocal.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hyperlocal.CriteriaData
import com.example.hyperlocal.OnboardingViewModel
import com.example.ui.onboarding.OnboardingGenderScreen
import com.example.ui.onboarding.OnboardingVibeScreen

object Routes {
    const val GENDER_SELECTION = "gender"
    const val MY_VIBE_SELECTION = "my_vibe"
    const val THEIR_VIBE_SELECTION = "their_vibe"
    const val MAIN_SCREEN = "main"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.GENDER_SELECTION) {
        composable(Routes.GENDER_SELECTION) {
            OnboardingGenderScreen { gender ->
                onboardingViewModel.onGenderSelected(gender)
                navController.navigate(Routes.MY_VIBE_SELECTION)
            }
        }
        composable(Routes.MY_VIBE_SELECTION) {
            OnboardingVibeScreen(
                title = "Step 2: My Vibe",
                criteria = CriteriaData.allCriteria
            ) { indices ->
                onboardingViewModel.onMyVibesSelected(indices)
                navController.navigate(Routes.THEIR_VIBE_SELECTION)
            }
        }
        composable(Routes.THEIR_VIBE_SELECTION) {
            OnboardingVibeScreen(
                title = "Step 3: Their Vibe",
                criteria = CriteriaData.allCriteria
            ) { indices ->
                onboardingViewModel.onTheirVibesSelected(indices)
                // TODO: Save the completed profile
                navController.navigate(Routes.MAIN_SCREEN) {
                    // Clear the onboarding back stack
                    popUpTo(Routes.GENDER_SELECTION) { inclusive = true }
                }
            }
        }
        composable(Routes.MAIN_SCREEN) {
            MainScreen()
        }
    }
}