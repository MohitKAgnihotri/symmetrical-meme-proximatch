package com.proxilocal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ActionBottomBar(
    isSweeping: Boolean,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit
) {
    // We remove the Surface and glassmorphism modifier completely.
    // A simple Box is used to position the button at the bottom center.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp), // Add some padding from the absolute bottom
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isSweeping) {
            // Show Stop button as a solid, semi-transparent button
            Button(
                onClick = onStopClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Stop")
            }
        } else {
            // Show Start button as a solid, themed button
            Button(
                onClick = onStartClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE) // A nice purple from your theme
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Start")
            }
        }
    }
}