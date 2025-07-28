// file: ui/components/MatchList.kt
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.Gender
import com.example.hyperlocal.MatchResult

@Composable
fun MatchList(matches: List<MatchResult>) {
    LazyColumn {
        items(matches) { match ->
            Card(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- FIX: The logic here is updated to use 'match.gender' ---
                    val indicatorColor = when (match.gender) {
                        Gender.MALE -> Color(0xFF03A9F4) // Blue
                        Gender.FEMALE -> Color(0xFFE91E63) // Pink
                        Gender.LGBTQ_PLUS -> Color(0xFF9C27B0) // Purple
                        Gender.PRIVATE -> Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(indicatorColor)
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