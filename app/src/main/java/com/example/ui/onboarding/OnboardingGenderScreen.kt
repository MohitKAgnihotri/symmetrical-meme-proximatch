package com.example.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hyperlocal.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingGenderScreen(
    onGenderSelected: (Gender) -> Unit
) {
    var selectedGender by remember { mutableStateOf<Gender?>(null) }
    val genders = Gender.values()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Step 1: Select Your Gender") }) },
        bottomBar = {
            Button(
                onClick = { selectedGender?.let(onGenderSelected) },
                enabled = selectedGender != null,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Next")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "This helps us match you with the right people.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genders) { gender ->
                    SelectableChip(
                        text = gender.name.replace("_", "+").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        isSelected = selectedGender == gender,
                        onClick = { selectedGender = gender }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
    }
}