package com.example.hyperlocal

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

data class LoginUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        // Check if a user is already signed in
        _uiState.value = LoginUiState(user = auth.currentUser)
    }

    // Generic sign-in method for any credential type
    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val result = auth.signInWithCredential(credential).await()
                _uiState.value = LoginUiState(user = result.user)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(error = e.message)
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = LoginUiState()
    }

    fun updateUser(user: FirebaseUser?) {
        _uiState.value = _uiState.value.copy(user = user)
    }
}