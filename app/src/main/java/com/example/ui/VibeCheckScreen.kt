package com.example.hyperlocal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hyperlocal.CriteriaManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeCheckScreen(navController: NavController) {
    val context = LocalContext.current
    val criteria = remember { CriteriaData.defaultCriteria }
    val selectedIndices = remember { mutableStateListOf<Int>() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vibe Check") })
        },
        bottomBar = {
            Button(
                onClick = {
                    CriteriaManager.saveUserPreferences(context, selectedIndices)
                    navController.navigate("main") // Navigate to MainActivity equivalent
                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Start Matching")
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            itemsIndexed(criteria) { index, item ->
                val selected = selectedIndices.contains(index)
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) selectedIndices.remove(index)
                        else selectedIndices.add(index)
                    },
                    label = { Text(item) },
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
