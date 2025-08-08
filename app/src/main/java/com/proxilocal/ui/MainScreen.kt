package com.proxilocal.ui

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseUser
import com.proxilocal.hyperlocal.InterestManager
import com.proxilocal.hyperlocal.LikeType
import com.proxilocal.hyperlocal.MainViewModel
import com.proxilocal.hyperlocal.MatchResult
import com.proxilocal.hyperlocal.MatchUiState
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
    val matchesUi by viewModel.matchUiList.collectAsState()   // UI state for radar
    val matchesRaw by viewModel.matchResults.collectAsState() // raw list for textual list
    val userLocation by viewModel.userLocation.collectAsState()
    val isSweeping by viewModel.isSweeping.collectAsState()
    val vmPermissionError by viewModel.permissionError.collectAsState()

    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    /* ───────────── Phase 6 events: toasts / navigation ───────────── */
    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is MainViewModel.UiEvent.ToastMsg ->
                    Toast.makeText(context, ev.msg, Toast.LENGTH_SHORT).show()
                is MainViewModel.UiEvent.NavigateToChat -> {
                    // TODO: wire to NavController if/when you have a chat route
                    // navController.navigate("chat/${ev.peerId}")
                }
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "Permissions result: $results")
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Permissions granted. Starting…", Toast.LENGTH_SHORT).show()
            viewModel.start(context)
        } else {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(vmPermissionError) {
        val missing = vmPermissionError
        if (!missing.isNullOrEmpty()) {
            Log.d(TAG, "VM reported missing permissions: $missing")
            permissionsLauncher.launch(missing.toTypedArray())
            viewModel.clearPermissionError()
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
        // 1) Full-screen map background
        MapBackground(userLocation = userLocation, modifier = Modifier.fillMaxSize())

        // 2) Scaffold chrome & content
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
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = Color.White
                                )
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
                            viewModel.start(context)
                            Toast.makeText(context, "Scanning Started", Toast.LENGTH_SHORT).show()
                        } else {
                            permissionsLauncher.launch(getRequiredPermissions())
                        }
                    },
                    onStopClicked = {
                        viewModel.stop()
                        Toast.makeText(context, "Scanning Stopped", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        ) { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                if (matchesRaw.isEmpty() && !isSweeping) {
                    Text(
                        text = "Press 'Start' to scan for nearby users.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (matchesRaw.isNotEmpty()) {
                    MatchList(
                        matches = matchesRaw,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }

        // 3) Centered radar overlay — pass rings callbacks for Phase 6
        CenteredRadarOverlay(
            theme = theme,
            matches = matchesUi,
            isSweeping = isSweeping,
            sideMinDp = 240.dp,
            sideScale = 0.95f,
            onDotTapped = { tappedUi -> selectedMatch = tappedUi.match },
            canTapRings = viewModel::isMutual,
            onRingsTap = { id -> viewModel.onRingsTapped(id) }
        )

        // 4) Dialog above everything
        selectedMatch?.let { match ->
            InterestDialog(
                match = match,
                onDismiss = { selectedMatch = null },
                onConfirm = { confirmed ->
                    InterestManager.saveInterest(context, confirmed.id)
                    viewModel.sendLikeTo(confirmed.id, LikeType.LIKE)
                    Toast.makeText(context, "Interest sent", Toast.LENGTH_SHORT).show()
                    selectedMatch = null
                }
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun CenteredRadarOverlay(
    theme: com.proxilocal.ui.components.RadarTheme,
    matches: List<MatchUiState>,
    isSweeping: Boolean,
    sideMinDp: Dp,
    sideScale: Float = 0.8f, // 0..1 of the screen's short side
    onDotTapped: (MatchUiState) -> Unit,
    // NEW for Phase 6:
    canTapRings: (String) -> Boolean,
    onRingsTap: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
            val shortSide = minOf(maxWidth, maxHeight)
            val side = (shortSide * sideScale).coerceAtLeast(sideMinDp)

            Box(
                modifier = Modifier
                    .size(side)
                    .aspectRatio(1f)
            ) {
                RadarCanvas(
                    theme = theme,
                    matches = matches,
                    isSweeping = isSweeping,
                    onDotTapped = onDotTapped,
                    // Phase 6: these two enable ring-tap connect
                    canTapRings = canTapRings,
                    onRingsTap = onRingsTap,
                    modifier = Modifier.fillMaxSize()
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
        confirmButton = { Button(onClick = onConfirm) { Text("Open Settings") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
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
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

private fun checkBLEPermissions(context: Context): Boolean {
    return getRequiredPermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
