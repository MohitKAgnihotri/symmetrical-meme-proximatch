package com.proxilocal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.proxilocal.hyperlocal.ui.theme.Black
import com.proxilocal.hyperlocal.ui.theme.Purple500
import com.proxilocal.hyperlocal.ui.theme.Teal200
import com.proxilocal.hyperlocal.ui.theme.Typography
import com.proxilocal.hyperlocal.ui.theme.White

private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = White,
    secondary = Teal200,
    onSecondary = Black
)

@Composable
fun HyperLocalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}