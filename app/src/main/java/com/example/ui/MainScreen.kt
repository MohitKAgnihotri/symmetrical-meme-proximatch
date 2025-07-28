package com.example.hyperlocal.ui

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

    // State to control the visibility of our permission explanation dialog
    var showPermissionRationale by remember { mutableStateOf(false) }

    // This launcher handles the result of the permission request dialog
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            Toast.makeText(context, "Permissions granted. Starting...", Toast.LENGTH_SHORT).show()
            viewModel.start(context)
        } else {
            // If permissions were denied, show our explanation
            showPermissionRationale = true
        }
    }

    // This composable will show the dialog when the state is true
    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onConfirm = {
                showPermissionRationale = false
                // Create an intent to open the app's settings screen
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            },
            onDismiss = { showPermissionRationale = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { ProxiMatchTopAppBar() },
            bottomBar = {
                ActionBottomBar(
                    onStartClicked = {
                        val permissions = getRequiredPermissions()
                        val allPermissionsGranted = permissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (allPermissionsGranted) {
                            viewModel.start(context)
                            Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
                        } else {
                            // This checks if the user has permanently denied the permission
                            val shouldShowRationale = permissions.any {
                                ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, it)
                            }
                            if (shouldShowRationale) {
                                // If so, show our custom explanation dialog
                                showPermissionRationale = true
                            } else {
                                // Otherwise, show the system permission request dialog
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
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .fillMaxSize()
            ) {
                RadarCanvas(
                    theme = theme,
                    matches = matches,
                    onDotTapped = { tapped -> selectedMatch = tapped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (matches.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Press 'Start' to scan for nearby users.\nEnsure permissions are granted.",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    MatchList(matches = matches)
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