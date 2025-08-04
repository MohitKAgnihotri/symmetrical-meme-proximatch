// UPDATED WITH VERBOSE LOGGING: LoginViewModel.kt
package com.proxilocal.hyperlocal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Holds UI‑state for the login screen.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

/**
 * Lightweight ViewModel that wraps FirebaseAuth sign‑in / sign‑out and exposes
 * a single [LoginUiState] flow.  ✳️  Added **verbose logging** for easier debugging.
 */
class LoginViewModel : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        // Emit current user on startup (if any)
        val existing = auth.currentUser
        Log.d(TAG, "init ➜ currentUser=${existing?.uid ?: "null"}")
        _uiState.value = LoginUiState(user = existing)
    }

    /**
     * Generic sign‑in for Google / GitHub or any other [AuthCredential].
     */
    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            Log.d(TAG, "signInWithCredential: provider=${credential.provider}")
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val result = auth.signInWithCredential(credential).await()
                Log.i(TAG, "signIn SUCCESS ➜ uid=${result.user?.uid}")
                _uiState.value = LoginUiState(user = result.user, isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "signIn FAILURE", e)
                _uiState.value = LoginUiState(error = e.localizedMessage, isLoading = false)
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut() called. uid=${auth.currentUser?.uid}")
        auth.signOut()
        _uiState.value = LoginUiState()
    }

    /**
     * Allows UI to update the current FirebaseUser after a silent sign‑in.
     */
    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser ➜ ${user?.uid ?: "null"}")
        _uiState.value = _uiState.value.copy(user = user, isLoading = false)
    }

    /**
     * Helper for surfacing manual error messages (e.g., ID‑token == null).
     */
    fun updateError(message: String) {
        Log.w(TAG, "updateError ➜ $message")
        _uiState.value = LoginUiState(error = message, isLoading = false)
    }
}
