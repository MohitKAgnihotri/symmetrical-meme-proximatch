package com.proxilocal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiMatchTopAppBar() {
    // Wrap the TopAppBar in a styled Surface
    Surface(
        color = Color.Black.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // --- FIX: Apply the glassmorphism modifier here ---
            .glassmorphism()
    ) {
        TopAppBar(
            title = { Text("ProxiMatch Radar") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
            )
        )
    }
}