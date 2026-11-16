package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkColorScheme = darkColorScheme(
    primary = BroadcastCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1214),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = InvestigativeAmber,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF3E2B04),
    onSecondaryContainer = Color(0xFFFFDEA6),
    tertiary = SignalCyan,
    onTertiary = Color(0xFF001F26),
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSlate,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = StudioSlate,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioCardElevated,
    outline = StudioBorder,
    error = BroadcastCrimsonGlow,
    onError = Color.White
)

@Composable
fun NewsCutStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
