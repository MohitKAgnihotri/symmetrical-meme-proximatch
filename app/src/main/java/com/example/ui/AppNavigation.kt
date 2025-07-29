package com.example.hyperlocal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hyperlocal.*
import com.example.ui.MainScreen
import com.example.ui.onboarding.OnboardingGenderScreen
import com.example.ui.onboarding.OnboardingVibeScreen
import com.example.ui.onboarding.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
    const val GENDER_SELECTION = "gender"
    const val MY_VIBE_SELECTION = "my_vibe"
    const val THEIR_VIBE_SELECTION = "their_vibe"
    const val MAIN_SCREEN = "main"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current

    // Check if a profile already exists to determine the start destination
    val startDestination = if (CriteriaManager.getUserProfile(context) != null) {
        Routes.MAIN_SCREEN
    } else {
        Routes.WELCOME
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen {
                navController.navigate(Routes.GENDER_SELECTION)
            }
        }
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

                // Assemble and save the completed profile
                val finalGender = onboardingViewModel.gender.value
                val myVibes = onboardingViewModel.myCriteria.value
                val theirVibes = indices // Use the indices from this final step

                if (finalGender != null) {
                    val userProfile = UserProfile(
                        gender = finalGender,
                        myCriteria = List(64) { i -> i in myVibes },
                        theirCriteria = List(64) { i -> i in theirVibes }
                    )
                    CriteriaManager.saveUserProfile(context, userProfile)
                }

                // Navigate to the main screen and clear the entire onboarding back stack
                navController.navigate(Routes.MAIN_SCREEN) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.MAIN_SCREEN) {
            MainScreen()
        }
    }
}