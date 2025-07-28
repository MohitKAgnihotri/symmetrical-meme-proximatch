package com.example.hyperlocal.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hyperlocal.Gender
import com.example.hyperlocal.InterestManager
import com.example.hyperlocal.MainViewModel
import com.example.hyperlocal.MatchResult
import com.example.hyperlocal.ui.components.ActionBottomBar
import com.example.hyperlocal.ui.components.InterestDialog
import com.example.hyperlocal.ui.components.ProxiMatchTopAppBar
import com.example.ui.components.* // Ensure this imports from the correct package

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val theme by viewModel.selectedTheme.collectAsState()

    // Create Sample Data for Testing the new Radar Visuals
    val sampleMatches = remember {
        listOf(
            MatchResult("user1", 95, -40, Gender.FEMALE),
            MatchResult("user2", 20, -85, Gender.MALE),
            MatchResult("user3", 60, -65, Gender.LGBTQ_PLUS),
            MatchResult("user4", 90, -80, Gender.PRIVATE),
            MatchResult("user5", 55, -55, Gender.MALE)
        )
    }

    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }

    // --- The Main UI is now a Box for layering ---
    Box(modifier = Modifier.fillMaxSize()) {
        // --- Layer 1: The Map Background ---
        MapBackground(modifier = Modifier.fillMaxSize())

        // --- Layer 2: The Main UI Scaffold ---
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { ProxiMatchTopAppBar() },
            bottomBar = {
                // This will now correctly call the implementation from your components package
                ActionBottomBar(
                    onStartClicked = {
                        if (checkBLEPermissions(context)) {
                            viewModel.start(context)
                            Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Missing permissions", Toast.LENGTH_SHORT).show()
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
                    matches = sampleMatches,
                    onDotTapped = { tapped -> selectedMatch = tapped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                MatchList(matches = sampleMatches)
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

// --- The faulty function that was here has been DELETED ---

fun checkBLEPermissions(context: Context): Boolean {
    val required = listOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    return required.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}