package com.proxilocal.hyperlocal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = White,
    secondary = Teal200,
    onSecondary = Black
)

@Composable
fun HyperLocalTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Purple500,
            darkIcons = false
        )
    }

    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}