package com.example.ui.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    premiumViewModel: PremiumViewModel = viewModel()
) {
    var discountCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val product by premiumViewModel.product.collectAsState()
    val isSubscribed by premiumViewModel.isSubscribed.collectAsState()
    val error by premiumViewModel.error.collectAsState()

    // Show a toast for any errors
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ProxiMatch Premium") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Premium",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Unlock Your Full Potential",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Go premium to get the most out of ProxiMatch and make more meaningful connections.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Feature List
            FeatureRow(text = "Expanded Criteria: Use up to 64 criteria points.")
            FeatureRow(text = "Advanced Filters: See only the matches you want.")
            FeatureRow(text = "\"Second Look\": Revisit profiles you've passed.")
            FeatureRow(text = "Priority Beacon: Boost your visibility in crowded areas.")

            Spacer(modifier = Modifier.weight(1f))

            // Pricing
            if (product != null && !isSubscribed) {
                Text(
                    "Only ${product!!.formattedPrice}/month",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            } else if (isSubscribed) {
                Text(
                    "You are a Premium Member!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discount Code
            if (!isSubscribed) {
                OutlinedTextField(
                    value = discountCode,
                    onValueChange = { discountCode = it },
                    label = { Text("Discount Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    premiumViewModel.launchPurchaseFlow(context as Activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = product != null && !isSubscribed
            ) {
                if (product == null && !isSubscribed) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else if (isSubscribed) {
                    Text("Subscribed", fontSize = 18.sp)
                } else {
                    Text("Subscribe Now", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}