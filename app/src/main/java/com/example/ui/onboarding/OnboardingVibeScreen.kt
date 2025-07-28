package com.example.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.Criterion

// FIX: Add this annotation to use FlowRow
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingVibeScreen(
    title: String,
    criteria: List<Criterion>,
    onVibesSelected: (List<Int>) -> Unit
) {
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val groupedCriteria = criteria.groupBy { it.category }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = {
            Button(
                onClick = { onVibesSelected(selectedIndices.toList()) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Next")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedCriteria.forEach { (category, items) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // FIX: Use horizontalArrangement for spacing
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items.forEach { criterion ->
                            val index = criteria.indexOf(criterion)
                            val isSelected = selectedIndices.contains(index)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                    } else if (criterion.isPremium) {
                                        // TODO: Add logic for premium upsell
                                    } else {
                                        selectedIndices.add(index)
                                    }
                                },
                                label = { Text(criterion.name) },
                                leadingIcon = if (criterion.isPremium) {
                                    { Icon(Icons.Default.Lock, contentDescription = "Premium") }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}