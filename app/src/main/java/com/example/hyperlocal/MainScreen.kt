package com.example.hyperlocal.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hyperlocal.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val matches by viewModel.matchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ProxiMatch Radar") })
        },
        bottomBar = {
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (hasPermissions(context)) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(8.dp)
                .fillMaxSize()
        ) {
            RadarCanvas(matches = matches)
            Spacer(modifier = Modifier.height(16.dp))
            MatchList(matches = matches)
        }
    }
}

@Composable
fun RadarCanvas(matches: List<MatchResult>) {
    val center = 200f
    val spacing = 50f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            matches.forEachIndexed { index, match ->
                val angle = Math.toRadians((index * 360.0 / matches.size))
                val radius = spacing * (index + 1)
                val x = center + radius * kotlin.math.cos(angle).toFloat()
                val y = center + radius * kotlin.math.sin(angle).toFloat()
                val color = when (match.colorCode) {
                    "Green" -> Color.Green
                    "Yellow" -> Color.Yellow
                    else -> Color.Gray
                }
                drawCircle(color = color, radius = 20f, center = Offset(x, y))
            }
        }
    }
}

@Composable
fun MatchList(matches: List<MatchResult>) {
    LazyColumn {
        items(matches) { match ->
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
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

fun hasPermissions(context: Context): Boolean {
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
