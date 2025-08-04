// FINAL PATCH: AppNavigation.kt (with safer LaunchedEffect)
package com.proxilocal.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import com.proxilocal.hyperlocal.*
import com.proxilocal.hyperlocal.R
import com.proxilocal.ui.onboarding.OnboardingGenderScreen
import com.proxilocal.ui.onboarding.OnboardingVibeScreen
import com.proxilocal.ui.onboarding.WelcomeScreen
import com.proxilocal.ui.premium.PremiumScreen
import kotlinx.coroutines.delay

object Routes {
    const val WELCOME = "welcome"
    const val GENDER_SELECTION = "gender"
    const val MY_VIBE_SELECTION = "my_vibe"
    const val THEIR_VIBE_SELECTION = "their_vibe"
    const val MAIN_SCREEN = "main"
    const val LOGIN = "login"
    const val PREMIUM = "premium"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current
    val auth: FirebaseAuth = Firebase.auth

    NavHost(navController = navController, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onContinueAnonymously = {
                    val profile = CriteriaManager.getUserProfile(context)
                    if (profile.gender == Gender.PRIVATE ||
                        profile.myCriteria.all { !it } ||
                        profile.theirCriteria.all { !it }) {
                        navController.navigate(Routes.GENDER_SELECTION)
                    } else {
                        navController.navigate(Routes.MAIN_SCREEN)
                    }
                }
            )
        }
        composable(Routes.GENDER_SELECTION) {
            OnboardingGenderScreen(
                onGenderSelected = { gender ->
                    onboardingViewModel.onGenderSelected(gender)
                    navController.navigate(Routes.MY_VIBE_SELECTION)
                },
                onLoginRequested = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.MY_VIBE_SELECTION) {
            OnboardingVibeScreen(
                title = "Step 2: My Vibe",
                criteria = CriteriaData.allCriteria,
                onVibesSelected = { indices ->
                    onboardingViewModel.onMyVibesSelected(indices)
                    navController.navigate(Routes.THEIR_VIBE_SELECTION)
                },
                onLoginRequested = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.THEIR_VIBE_SELECTION) {
            OnboardingVibeScreen(
                title = "Step 3: Their Vibe",
                criteria = CriteriaData.allCriteria,
                onVibesSelected = { indices ->
                    onboardingViewModel.onTheirVibesSelected(indices)
                    val finalGender = onboardingViewModel.gender.value
                    val myVibes = onboardingViewModel.myCriteria.value
                    if (finalGender != null) {
                        val userProfile = UserProfile(
                            gender = finalGender,
                            myCriteria = List(64) { i -> i in myVibes },
                            theirCriteria = List(64) { i -> i in indices }
                        )
                        CriteriaManager.saveUserProfile(context, userProfile)
                    }
                    navController.navigate(Routes.MAIN_SCREEN) { popUpTo(Routes.WELCOME) { inclusive = true } }
                },
                onLoginRequested = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.MAIN_SCREEN) {
            val loginViewModel: LoginViewModel = viewModel()
            val userState by loginViewModel.uiState.collectAsState()

            val profile = CriteriaManager.getUserProfile(context)
            val isProfileValid = profile.gender != Gender.PRIVATE &&
                    profile.myCriteria.any { it } && profile.theirCriteria.any { it }

            if (!isProfileValid) {
                navController.navigate(Routes.GENDER_SELECTION)
            } else {
                MainScreen(
                    user = userState.user ?: auth.currentUser,
                    onGoToLogin = { navController.navigate(Routes.LOGIN) },
                    onLogout = {
                        loginViewModel.signOut()
                        navController.navigate(Routes.WELCOME) { popUpTo(Routes.MAIN_SCREEN) { inclusive = true } }
                    }
                )
            }
        }
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel()
            val uiState by loginViewModel.uiState.collectAsState()
            val currentUser by rememberUpdatedState(uiState.user)

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    try {
                        val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
                        val googleIdToken = credential.googleIdToken
                        if (googleIdToken != null) {
                            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                            loginViewModel.signInWithCredential(firebaseCredential)
                        }
                    } catch (e: ApiException) {
                        Log.w("AppNavigation", "Google sign in failed", e)
                    }
                }
            }

            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    delay(200)
                    val profile = CriteriaManager.getUserProfile(context)
                    val isProfileValid = profile.gender != Gender.PRIVATE &&
                            profile.myCriteria.any { it } && profile.theirCriteria.any { it }

                    navController.navigate(
                        if (isProfileValid) Routes.MAIN_SCREEN else Routes.GENDER_SELECTION
                    ) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onGoogleSignIn = {
                    val signInClient = Identity.getSignInClient(context)
                    val request = com.google.android.gms.auth.api.identity.GetSignInIntentRequest.builder()
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .build()
                    signInClient.getSignInIntent(request)
                        .addOnSuccessListener { result ->
                            googleSignInLauncher.launch(IntentSenderRequest.Builder(result).build())
                        }
                        .addOnFailureListener { e ->
                            Log.e("AppNavigation", "Google sign-in intent failed", e)
                        }
                },
                onGitHubSignIn = {
                    val provider = OAuthProvider.newBuilder("github.com")
                    auth.startActivityForSignInWithProvider(context as Activity, provider.build())
                        .addOnSuccessListener { result -> loginViewModel.updateUser(result.user) }
                        .addOnFailureListener { e -> Log.w("AppNavigation", "GitHub sign in failed", e) }
                },
                onGoToPremium = { navController.navigate(Routes.PREMIUM) },
                onDismiss = { navController.popBackStack() }
            )
        }
        composable(Routes.PREMIUM) {
            PremiumScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
