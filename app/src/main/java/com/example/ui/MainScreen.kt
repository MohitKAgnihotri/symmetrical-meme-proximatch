package com.example.hyperlocal.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hyperlocal.Gender
import com.example.hyperlocal.InterestManager
import com.example.hyperlocal.MainViewModel
import com.example.hyperlocal.MatchResult
import com.example.hyperlocal.ui.components.*
import com.example.ui.components.MatchList
import com.example.ui.components.RadarCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    // This would eventually come from the ViewModel, but we use sample data for now.
    // val matches by viewModel.matchResults.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()

    // Create Sample Data for Testing the new Radar Visuals
    val sampleMatches = remember {
        listOf(
            // High-match, female, close distance -> should be vibrant pink, pulsing, near center
            MatchResult("user1", 95, -40, Gender.FEMALE),
            // Low-match, male, far distance -> should be pale blue, far from center
            MatchResult("user2", 20, -85, Gender.MALE),
            // Mid-match, LGBTQ+, mid distance -> should be rainbow, mid-way out
            MatchResult("user3", 60, -65, Gender.LGBTQ_PLUS),
            // High-match, private, far distance -> should be gray, pulsing, far out
            MatchResult("user4", 90, -80, Gender.PRIVATE),
            // Another mid-match male for variety
            MatchResult("user5", 55, -55, Gender.MALE)
        )
    }

    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }

    Scaffold(
        topBar = { ProxiMatchTopAppBar() },
        bottomBar = {
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
                matches = sampleMatches, // <-- Using the sample data
                onDotTapped = { tapped -> selectedMatch = tapped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            MatchList(matches = sampleMatches) // <-- Using the sample data
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