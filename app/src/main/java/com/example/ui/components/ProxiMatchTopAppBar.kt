// file: ui/components/ProxiMatchTopAppBar.kt
package com.example.hyperlocal.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiMatchTopAppBar() {
    TopAppBar(title = { Text("ProxiMatch Radar") })
}