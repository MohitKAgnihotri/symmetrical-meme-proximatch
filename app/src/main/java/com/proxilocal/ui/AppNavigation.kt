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

    val startDestination = when {
        auth.currentUser != null -> Routes.MAIN_SCREEN
        CriteriaManager.getUserProfile(context) != null -> Routes.MAIN_SCREEN
        else -> Routes.WELCOME
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onContinueAnonymously = { navController.navigate(Routes.GENDER_SELECTION) }
            )
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
                if (finalGender != null) {
                    val userProfile = UserProfile(
                        gender = finalGender,
                        myCriteria = List(64) { i -> i in myVibes },
                        theirCriteria = List(64) { i -> i in indices }
                    )
                    CriteriaManager.saveUserProfile(context, userProfile)
                }
                navController.navigate(Routes.MAIN_SCREEN) { popUpTo(Routes.WELCOME) { inclusive = true } }
            }
        }
        composable(Routes.MAIN_SCREEN) {
            val loginViewModel: LoginViewModel = viewModel()
            val userState by loginViewModel.uiState.collectAsState()
            MainScreen(
                user = userState.user ?: auth.currentUser,
                onGoToLogin = { navController.navigate(Routes.LOGIN) },
                onLogout = {
                    loginViewModel.signOut()
                    navController.navigate(Routes.WELCOME) { popUpTo(Routes.MAIN_SCREEN) { inclusive = true } }
                }
            )
        }
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel()
            val uiState by loginViewModel.uiState.collectAsState()

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

            LaunchedEffect(uiState) {
                if (uiState.user != null) {
                    if (CriteriaManager.getUserProfile(context) == null) {
                        navController.navigate(Routes.GENDER_SELECTION) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    } else {
                        navController.navigate(Routes.MAIN_SCREEN) { popUpTo(Routes.WELCOME) { inclusive = true } }
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