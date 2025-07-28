package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.Gender
import com.example.hyperlocal.MatchResult

@Composable
fun MatchList(matches: List<MatchResult>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp) // Add more space
    ) {
        items(matches) { match ->
            MatchListItem(match = match)
        }
    }
}

@Composable
private fun MatchListItem(match: MatchResult) {
    val baseColor = when (match.gender) {
        Gender.MALE -> Color(0xFF03A9F4)
        Gender.FEMALE -> Color(0xFFE91E63)
        Gender.LGBTQ_PLUS -> Color(0xFF9C27B0)
        Gender.PRIVATE -> Color.Gray
    }

    val indicatorColor = lerp(
        start = Color.Gray.copy(alpha = 0.5f),
        stop = baseColor,
        fraction = match.matchPercentage / 100f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Match Candidate #${match.id.take(4)}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "${match.matchPercentage}%",
                style = MaterialTheme.typography.bodyLarge,
                color = indicatorColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { match.matchPercentage / 100f }, // Use lambda for progress
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = indicatorColor,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}