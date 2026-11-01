package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// HWX Gateway Core Palette
val HWXBackground = Color(0xFF050B18)
val HWXSecondaryBackground = Color(0xFF081329)
val HWXCardBackground = Color(0xFF101B32)
val HWXCardSurface = Color(0xFF15223E)
val HWXCardBorder = Color(0xFF1F2F52)
val HWXCardBorderGlow = Color(0x3300D5FF)

val HWXPrimaryBlue = Color(0xFF087BFF)
val HWXCyan = Color(0xFF00D5FF)
val HWXEmeraldGreen = Color(0xFF00E69A)
val HWXDarkGreen = Color(0xFF004D34)

val HWXTextPrimary = Color(0xFFFFFFFF)
val HWXTextSecondary = Color(0xFFA8B5CC)
val HWXTextMuted = Color(0xFF6B7A99)

val HWXError = Color(0xFFFF4565)
val HWXWarning = Color(0xFFFFB800)
val HWXSuccess = Color(0xFF00E69A)

// Gradient Brushes
val HWXPrimaryGradient = Brush.horizontalGradient(
    colors = listOf(HWXPrimaryBlue, HWXCyan)
)

val HWXCyanGreenGradient = Brush.horizontalGradient(
    colors = listOf(HWXCyan, HWXEmeraldGreen)
)

val HWXBlueGreenGradient = Brush.linearGradient(
    colors = listOf(HWXPrimaryBlue, HWXEmeraldGreen)
)

val HWXCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF15223E), Color(0xFF0D172A))
)

val HWXServerStatusGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF063326), Color(0xFF0B4635))
)
