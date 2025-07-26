// MainScreen.kt
package com.example.hyperlocal.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hyperlocal.InterestManager
import com.example.hyperlocal.MainViewModel
import com.example.hyperlocal.MatchResult
import com.example.hyperlocal.ui.RadarCanvas
import com.example.hyperlocal.ui.ThemeProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe the list of matches and the selected theme
    val matches by viewModel.matchResults.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()

    // State for the tapped dot
    var selectedMatch by remember { mutableStateOf<MatchResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ProxiMatch Radar") })
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (checkBLEPermissions(context)) {
                        viewModel.start(context)
                        Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Missing permissions", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Start")
                }
                Button(onClick = {
                    viewModel.stop()
                    Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Stop")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxSize()
        ) {
            // Radar canvas with theming and tap handling
            RadarCanvas(
                theme = theme,
                matches = matches,
                onDotTapped = { tapped ->
                    selectedMatch = tapped
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of matches below
            MatchListUI(matches)
        }

        // Dialog when a dot is tapped
        selectedMatch?.let { match ->
            AlertDialog(
                onDismissRequest = { selectedMatch = null },
                title = { Text("Send Interest?") },
                text = { Text("This user won't know unless they also send interest to you.") },
                confirmButton = {
                    TextButton(onClick = {
                        InterestManager.saveInterest(context, match.id)
                        Toast.makeText(context, "Interest sent", Toast.LENGTH_SHORT).show()
                        selectedMatch = null
                    }) {
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedMatch = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MatchListUI(matches: List<MatchResult>) {
    LazyColumn {
        items(matches) { match ->
            Card(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                when (match.colorCode) {
                                    "Green" -> Color.Green
                                    "Yellow" -> Color.Yellow
                                    else     -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Match: ${match.matchPercentage}%",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
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
