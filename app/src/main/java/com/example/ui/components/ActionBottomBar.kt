package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ActionBottomBar(
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            // --- FIX: Apply the glassmorphism modifier here ---
            .glassmorphism()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onStartClicked,
                border = BorderStroke(1.dp, Color(0xFF03A9F4)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF03A9F4))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Start")
            }
            OutlinedButton(
                onClick = onStopClicked,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Stop")
            }
        }
    }
}