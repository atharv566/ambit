package com.example.cpvts.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Palette {
    val Canvas = Color(0xFFF9F7F2); val Beige = Color(0xFFF4EFE6)
    val White = Color(0xFFFFFFFF); val Border = Color(0xFFE6DFD5)
    val Ink = Color(0xFF231F20); val Muted = Color(0xFF736B63)
    val Amber = Color(0xFFFFB300); val AmberTint = Color(0xFFFFF8E1)
    val Blue = Color(0xFF1E88E5); val BlueTint = Color(0xFFE3F2FD)
    val Green = Color(0xFF2E7D32); val GreenTint = Color(0xFFE8F5E9)
}
private val colors = lightColorScheme(
    primary = Palette.Amber, onPrimary = Palette.Ink, primaryContainer = Palette.AmberTint, onPrimaryContainer = Palette.Ink,
    secondary = Palette.Blue, onSecondary = Palette.Ink, secondaryContainer = Palette.BlueTint, onSecondaryContainer = Palette.Ink,
    tertiary = Palette.Green, onTertiary = Palette.White, tertiaryContainer = Palette.GreenTint, onTertiaryContainer = Palette.Green,
    background = Palette.Canvas, onBackground = Palette.Ink, surface = Palette.White, onSurface = Palette.Ink,
    surfaceVariant = Palette.Beige, onSurfaceVariant = Palette.Muted, outline = Palette.Muted, outlineVariant = Palette.Border,
    error = Palette.Ink, onError = Palette.White, errorContainer = Palette.AmberTint, onErrorContainer = Palette.Ink,
    inverseSurface = Palette.Ink, inverseOnSurface = Palette.White, inversePrimary = Palette.Amber,
    surfaceTint = Palette.White, scrim = Palette.Ink,
    surfaceDim = Palette.Beige, surfaceBright = Palette.White,
    surfaceContainerLowest = Palette.White, surfaceContainerLow = Palette.Canvas, surfaceContainer = Palette.Beige,
    surfaceContainerHigh = Palette.Beige, surfaceContainerHighest = Palette.Beige
)
@Composable fun CpvtsTheme(content: @Composable () -> Unit) {
    // User's explicit light-only palette overrides the generic brief's dark-mode request.
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(
        small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp)
    ), content = content)
}
