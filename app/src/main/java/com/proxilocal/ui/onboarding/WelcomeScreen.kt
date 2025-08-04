// UPDATED: WelcomeScreen.kt
package com.proxilocal.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proxilocal.hyperlocal.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onContinueAnonymously: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to ProxiMatch",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Where real-life vibes meet instant, anonymous discovery.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            FeatureItem(
                icon = Icons.Rounded.PrivacyTip,
                title = "Privacy First",
                description = "No names, no profiles, no trails. You're in control."
            )
            Spacer(Modifier.height(16.dp))
            FeatureItem(
                icon = Icons.Rounded.WavingHand,
                title = "Local & Instant",
                description = "Connect with people physically nearby, right now."
            )
            Spacer(Modifier.height(16.dp))
            FeatureItem(
                icon = Icons.Rounded.FavoriteBorder,
                title = "Values-based Matching",
                description = "Set your vibe, define your match. That's it."
            )
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login / Register")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onContinueAnonymously,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue Anonymously")
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}