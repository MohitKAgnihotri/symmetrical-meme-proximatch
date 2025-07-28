package com.example.hyperlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hyperlocal.ui.AppNavigation
import com.example.hyperlocal.ui.theme.HyperLocalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HyperLocalTheme {
                AppNavigation()
            }
        }
    }
}