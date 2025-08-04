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
 * UI state for the login flow.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

/**
 * ViewModel that wraps FirebaseAuth sign‑in/out, with verbose logging
 * and convenience functions for loading & error handling.
 */
class LoginViewModel : ViewModel() {

    companion object { private const val TAG = "LoginViewModel" }

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        Log.d(TAG, "init ➜ currentUser=${auth.currentUser?.uid}")
        _uiState.value = LoginUiState(user = auth.currentUser)
    }

    /* ───── Public helpers ───── */

    /** Toggle the loading spinner */
    fun setLoading(flag: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = flag, error = null)
    }

    /** Push an error message & hide spinner */
    fun updateError(message: String) {
        Log.w(TAG, "updateError ➜ $message")
        _uiState.value = _uiState.value.copy(error = message, isLoading = false)
    }

    /** Update the current FirebaseUser (clears spinner) */
    fun updateUser(user: FirebaseUser?) {
        Log.d(TAG, "updateUser ➜ ${user?.uid}")
        _uiState.value = LoginUiState(user = user, isLoading = false)
    }

    /* ───── Auth actions ───── */

    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val result = auth.signInWithCredential(credential).await()
                Log.i(TAG, "signIn SUCCESS ➜ uid=${result.user?.uid}")
                updateUser(result.user)
            } catch (e: Exception) {
                Log.e(TAG, "signIn FAILURE", e)
                updateError(e.localizedMessage ?: "Authentication failed")
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut() uid=${auth.currentUser?.uid}")
        auth.signOut()
        _uiState.value = LoginUiState()
    }
}
