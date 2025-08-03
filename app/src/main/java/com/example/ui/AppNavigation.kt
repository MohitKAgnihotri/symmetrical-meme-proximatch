package com.example.hyperlocal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hyperlocal.*
import com.example.ui.LoginScreen
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
    const val LOGIN = "login"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current

    val startDestination = if (CriteriaManager.getUserProfile(context) != null) {
        Routes.MAIN_SCREEN
    } else {
        Routes.WELCOME
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // --- Onboarding Routes ---
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

                val finalGender = onboardingViewModel.gender.value
                val myVibes = onboardingViewModel.myCriteria.value
                val theirVibes = indices

                if (finalGender != null) {
                    val userProfile = UserProfile(
                        gender = finalGender,
                        myCriteria = List(64) { i -> i in myVibes },
                        theirCriteria = List(64) { i -> i in theirVibes }
                    )
                    CriteriaManager.saveUserProfile(context, userProfile)
                }

                navController.navigate(Routes.MAIN_SCREEN) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }

        // --- Main App Routes ---
        composable(Routes.MAIN_SCREEN) {
            // The error indicates your MainScreen composable does not accept an 'onGoToLogin' parameter.
            // You will need to add a button inside MainScreen that uses the NavController to navigate to Routes.LOGIN.
            MainScreen()
        }

        composable(Routes.LOGIN) {
            // The errors indicate your LoginScreen composable expects individual lambdas for each sign-in action
            // instead of a ViewModel. This has been corrected below.
            LoginScreen(
                onGoogleSignIn = { /* TODO: Implement Google Sign-In logic here or in a ViewModel */ },
                onAppleSignIn = { /* TODO: Implement Apple Sign-In */ },
                onFacebookSignIn = { /* TODO: Implement Facebook Sign-In */ },
                onInstagramSignIn = { /* TODO: Implement Instagram Sign-In */ },
                onGitHubSignIn = { /* TODO: Implement GitHub Sign-In */ },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
