package com.proxilocal.hyperlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proxilocal.ui.AppNavigation
import com.proxilocal.ui.theme.HyperLocalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HyperLocalTheme { // Added theme wrapper
                AppNavigation()
            }
        }
    }
}