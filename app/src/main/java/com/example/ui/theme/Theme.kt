package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HWXColorScheme = darkColorScheme(
    primary = HWXPrimaryBlue,
    onPrimary = HWXTextPrimary,
    secondary = HWXCyan,
    onSecondary = HWXBackground,
    tertiary = HWXEmeraldGreen,
    onTertiary = HWXBackground,
    background = HWXBackground,
    onBackground = HWXTextPrimary,
    surface = HWXCardBackground,
    onSurface = HWXTextPrimary,
    surfaceVariant = HWXCardSurface,
    onSurfaceVariant = HWXTextSecondary,
    outline = HWXCardBorder,
    error = HWXError,
    onError = HWXTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HWXColorScheme,
        typography = Typography,
        content = content
    )
}
