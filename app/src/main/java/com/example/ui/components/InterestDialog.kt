// file: ui/components/InterestDialog.kt
package com.example.hyperlocal.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.hyperlocal.MatchResult

@Composable
fun InterestDialog(
    match: MatchResult,
    onDismiss: () -> Unit,
    onConfirm: (MatchResult) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Interest?") },
        text = { Text("This user won't know unless they also send interest to you.") },
        confirmButton = {
            TextButton(onClick = { onConfirm(match) }) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}