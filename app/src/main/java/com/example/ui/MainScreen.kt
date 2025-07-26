package com.example.hyperlocal.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val matches by viewModel.matchResults.collectAsState()
    var selected by remember { mutableStateOf<MatchResult?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ProxiMatch Radar") }) },
        bottomBar = {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (checkBLEPermissions(context)) {
                        viewModel.start(context)
                        Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Missing permissions", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Start") }

                Button(onClick = {
                    viewModel.stop()
                    Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
                }) { Text("Stop") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(8.dp).fillMaxSize()
        ) {
            RadarCanvas(matches = matches, onDotTapped = { selected = it })
            Spacer(Modifier.height(16.dp))
            MatchListUI(matches)
        }

        selected?.let { match ->
            AlertDialog(
                onDismissRequest = { selected = null },
                confirmButton = {
                    TextButton(onClick = {
                        InterestManager.saveInterest(context, match.id)
                        Toast.makeText(context, "Interest sent", Toast.LENGTH_SHORT).show()
                        selected = null
                    }) { Text("Send") }
                },
                dismissButton = {
                    TextButton(onClick = { selected = null }) { Text("Cancel") }
                },
                title = { Text("Send Interest?") },
                text = { Text("This user won't know unless they also send interest to you.") }
            )
        }
    }
}

@Composable
fun MatchListUI(matches: List<MatchResult>) {
    LazyColumn {
        items(matches) { match ->
            Card(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
                Row(
                    Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(16.dp).background(
                            when (match.colorCode) {
                                "Green" -> Color.Green
                                "Yellow" -> Color.Yellow
                                else -> Color.Gray
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Match: ${match.matchPercentage}%", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

fun checkBLEPermissions(context: Context): Boolean {
    val perms = listOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    return perms.all {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
