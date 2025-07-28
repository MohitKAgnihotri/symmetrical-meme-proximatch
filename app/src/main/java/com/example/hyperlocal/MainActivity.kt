package com.example.hyperlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hyperlocal.ui.MainScreen // <-- FIX: Corrected the import path
import com.example.hyperlocal.ui.theme.HyperLocalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Best practice: Enable edge-to-edge display
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Best practice: Wrap your screen in your app's theme
            HyperLocalTheme {
                MainScreen()
            }
        }
    }
}