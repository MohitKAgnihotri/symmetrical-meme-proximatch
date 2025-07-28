package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.example.hyperlocal.InterestManager
import com.example.hyperlocal.MainViewModel
import com.example.hyperlocal.MatchResult
import com.example.hyperlocal.ui.components.ActionBottomBar
import com.example.hyperlocal.ui.components.InterestDialog
import com.example.hyperlocal.ui.components.ProxiMatchTopAppBar
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val theme by viewModel.selectedTheme.collectAsState()
    val matches by viewModel.matchResults.collectAsState()
    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "Permissions granted. Starting...", Toast.LENGTH_SHORT).show()
            viewModel.start(context)
        } else {
            showPermissionRationale = true
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
        // Layer 1: The Map Background
        MapBackground(modifier = Modifier.fillMaxSize())

        // --- FIX: Layer 2 is now the FULL-SCREEN transparent Radar Canvas ---
        RadarCanvas(
            theme = theme,
            matches = matches,
            onDotTapped = { tapped -> selectedMatch = tapped },
            modifier = Modifier.fillMaxSize() // Fills the whole screen
        )

        // --- FIX: Layer 3 is the Scaffold, containing only floating UI elements ---
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { ProxiMatchTopAppBar() },
            bottomBar = {
                ActionBottomBar(
                    onStartClicked = {
                        val permissions = getRequiredPermissions()
                        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            viewModel.start(context)
                            Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
                        } else {
                            if (permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, it) }) {
                                showPermissionRationale = true
                            } else {
                                permissionsLauncher.launch(permissions)
                            }
                        }
                    },
                    onStopClicked = {
                        viewModel.stop()
                        Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { paddingValues ->
            // The content of the scaffold is now just the list, pushed to the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            ) {
                if (matches.isEmpty()) {
                    Text(
                        text = "Press 'Start' to scan for nearby users.\nEnsure permissions are granted.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    MatchList(
                        matches = matches,
                        modifier = Modifier.align(Alignment.BottomCenter) // Align list to bottom
                    )
                }
            }
        }

        // The dialog will appear over everything
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

// ... (PermissionRationaleDialog and other helper functions remain the same)

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

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun checkBLEPermissions(context: Context): Boolean {
    return getRequiredPermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}