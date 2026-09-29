package com.rotherbaum.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RbColorScheme = darkColorScheme(
    primary = RbAccent,
    onPrimary = RbBackground,
    secondary = RbAccentSoft,
    background = RbBackground,
    surface = RbSurface,
    surfaceVariant = RbSurfaceElevated,
    onBackground = RbTextPrimary,
    onSurface = RbTextPrimary
)

@Composable
fun RotherbaumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RbColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
