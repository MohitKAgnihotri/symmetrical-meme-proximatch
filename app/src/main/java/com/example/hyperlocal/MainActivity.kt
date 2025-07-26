// MainActivity.kt (Compose-based with BLE, RadarCanvas, Match List, and Interest Dialog)
package com.example.hyperlocal

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.hyperlocal.ui.theme.HyperLocalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var bleScanner: BLEScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleAdvertiser = BLEAdvertiser(this)
        bleScanner = BLEScanner(this) { match ->
            MatchStore.add(match)
        }

        setContent {
            HyperLocalTheme {
                MainScreen(
                    onStart = {
                        if (hasPermissions()) {
                            val data = CriteriaManager.getEncodedCriteria(this)
                            bleAdvertiser.startAdvertising(data)
                            bleScanner.startScanning()
                            Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Missing permissions", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStop = {
                        bleAdvertiser.stopAdvertising()
                        bleScanner.stopScanning()
                        MatchStore.clear()
                        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val perms = listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return perms.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val matches by MatchStore.matches.collectAsState()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<MatchResult?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ProxiMatch Radar") }) },
        bottomBar = {
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onStart) { Text("Start") }
                Button(onClick = onStop) { Text("Stop") }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(8.dp)
                .fillMaxSize()
        ) {
            RadarCanvas(matches = matches, onSelect = { match ->
                if (!InterestManager.hasSentInterest(context, match.id)) {
                    selected = match
                }
            })
            Spacer(Modifier.height(16.dp))
            MatchList(matches)
        }

        selected?.let { match ->
            AlertDialog(
                onDismissRequest = { selected = null },
                confirmButton = {
                    TextButton(onClick = {
                        InterestManager.saveInterest(context, match.id)
                        Toast.makeText(context, "Interest sent", Toast.LENGTH_SHORT).show()
                        selected = null
                    }) {
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selected = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Send Interest?") },
                text = { Text("This user won't know unless they also send interest to you.") }
            )
        }
    }
}

@Composable
fun RadarCanvas(matches: List<MatchResult>, onSelect: (MatchResult) -> Unit) {
    val centerX = 200f
    val centerY = 200f
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
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                val color = when (match.colorCode) {
                    "Green" -> Color.Green
                    "Yellow" -> Color.Yellow
                    else -> Color.Gray
                }
                drawCircle(color, center = Offset(x, y), radius = 20f)
            }
        }

        matches.forEachIndexed { index, match ->
            val angle = Math.toRadians((index * 360.0 / matches.size))
            val radius = spacing * (index + 1)
            val x = radius * cos(angle).toFloat()
            val y = radius * sin(angle).toFloat()

            Box(
                modifier = Modifier
                    .offset { IntOffset((x + centerX).toInt(), (y + centerY).toInt()) }
                    .size(40.dp)
                    .clickable { onSelect(match) }
            )
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
                    Text("Match: ${match.matchPercentage}%")
                }
            }
        }
    }
}

object MatchStore {
    private val _matches = MutableStateFlow<List<MatchResult>>(emptyList())
    val matches = _matches.asStateFlow()

    fun add(match: MatchResult) {
        _matches.value = _matches.value + match
    }

    fun clear() {
        _matches.value = emptyList()
    }
}
