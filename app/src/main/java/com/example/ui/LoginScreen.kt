package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onFacebookSignIn: () -> Unit,
    onInstagramSignIn: () -> Unit,
    onGitHubSignIn: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Premium Features") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Sign In to Go Premium",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your login is only used to manage your subscription and does not affect your anonymous matching profile.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Social Login Buttons
            SocialLoginButton("Sign in with Google", R.drawable.ic_google_logo, onGoogleSignIn)
            Spacer(modifier = Modifier.height(16.dp))
            SocialLoginButton("Sign in with Apple", R.drawable.ic_apple_logo, onAppleSignIn)
            Spacer(modifier = Modifier.height(16.dp))
            SocialLoginButton("Sign in with Facebook", R.drawable.ic_facebook_logo, onFacebookSignIn)
            Spacer(modifier = Modifier.height(16.dp))
            SocialLoginButton("Sign in with Instagram", R.drawable.ic_instagram_logo, onInstagramSignIn)
            Spacer(modifier = Modifier.height(16.dp))
            SocialLoginButton("Sign in with GitHub", R.drawable.ic_github_logo, onGitHubSignIn)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialLoginButton(
    text: String,
    iconResId: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = text,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified // Use original colors of the logos
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface)
    }
}
