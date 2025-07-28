// file: ui/components/ActionBottomBar.kt
package com.example.hyperlocal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionBottomBar(
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onStartClicked) {
            Text("Start")
        }
        Button(onClick = onStopClicked) {
            Text("Stop")
        }
    }
}