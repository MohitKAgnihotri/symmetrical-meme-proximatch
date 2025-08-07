package com.proxilocal.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseUser
import com.proxilocal.hyperlocal.InterestManager
import com.proxilocal.hyperlocal.MainViewModel
import com.proxilocal.hyperlocal.MatchResult
import com.proxilocal.hyperlocal.ui.components.InterestDialog
import com.proxilocal.ui.components.*

private const val TAG = "MainScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    user: FirebaseUser?,
    onGoToLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val theme by viewModel.selectedTheme.collectAsState()
    val matches by viewModel.matchResults.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isSweeping by viewModel.isSweeping.collectAsState()
    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions result received: $permissions")
        if (permissions.values.all { it }) {
            Log.d(TAG, "All permissions GRANTED by user.")
            Toast.makeText(context, "Permissions granted. Starting...", Toast.LENGTH_SHORT).show()
            viewModel.start(context)
        } else {
            Log.w(TAG, "Some or all permissions DENIED by user.")
            showPermissionRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (checkBLEPermissions(context)) {
            viewModel.fetchUserLocation(context)
        }
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onConfirm = {
                showPermissionRationale = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            },
            onDismiss = { showPermissionRationale = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapBackground(userLocation = userLocation, modifier = Modifier.fillMaxSize())

        RadarCanvas(
            theme = theme,
            matches = matches,
            isSweeping = isSweeping,
            onDotTapped = { tapped -> selectedMatch = tapped },
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ProxiMatch Radar", color = Color.White) },
                    actions = {
                        if (user != null) {
                            Text(
                                text = user.email ?: "Logged In",
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = onGoToLogin) {
                                Icon(Icons.Default.Star, contentDescription = "Go Premium", tint = Color.Yellow)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                ActionBottomBar(
                    isSweeping = isSweeping,
                    onStartClicked = {
                        Log.d(TAG, "Start button clicked.")
                        if (checkBLEPermissions(context)) {
                            Log.d(TAG, "Permissions already granted. Starting ViewModel.")
                            viewModel.start(context)
                            Toast.makeText(context, "Scanning Started", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d(TAG, "Permissions NOT granted. Launching request.")
                            val permissions = getRequiredPermissions()
                            permissionsLauncher.launch(permissions)
                        }
                    },
                    onStopClicked = {
                        Log.d(TAG, "Stop button clicked.")
                        viewModel.stop()
                        Toast.makeText(context, "Scanning Stopped", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            ) {
                if (matches.isEmpty() && !isSweeping) {
                    Text(
                        text = "Press 'Start' to scan for nearby users.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (matches.isNotEmpty()) {
                    MatchList(
                        matches = matches,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        selectedMatch?.let { match ->
            InterestDialog(
                match = match,
                onDismiss = { selectedMatch = null },
                onConfirm = { confirmedMatch ->
                    InterestManager.saveInterest(context, confirmedMatch.id)
                    Toast.makeText(context, "Interest sent", Toast.LENGTH_SHORT).show()
                    selectedMatch = null
                }
            )
        }
    }
}

@Composable
fun PermissionRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = { Text("This app needs Bluetooth and Location permissions to find nearby users. Please grant them in the app settings.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// *** THIS IS THE CORRECTED FUNCTION ***
private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // For Android 12 (API 31) and newer
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        // For Android 11 (API 30) and older
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

fun checkBLEPermissions(context: Context): Boolean {
    val allPermissionsGranted = getRequiredPermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
    Log.d(TAG, "checkBLEPermissions result: $allPermissionsGranted")
    return allPermissionsGranted
}