package com.kisss.visualtime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VisualTimeColorScheme = darkColorScheme(
    primary = SandGold,
    onPrimary = TrueBlack,
    background = TrueBlack,
    onBackground = SoftWhite,
    surface = DarkCharcoal,
    onSurface = SoftWhite,
)

@Composable
fun VisualTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VisualTimeColorScheme,
        typography = VisualTimeTypography,
        content = content,
    )
}
