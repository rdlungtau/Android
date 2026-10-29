package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmblemGold,
    onPrimary = SaiphaiNavyDark,
    primaryContainer = SaiphaiNavyLight,
    onPrimaryContainer = EmblemGoldContainer,
    secondary = AutoGreenLight,
    onSecondary = SaiphaiNavyDark,
    secondaryContainer = Color(0xFF0A4723),
    onSecondaryContainer = AutoGreenContainer,
    tertiary = EmblemGold,
    onTertiary = SaiphaiNavyDark,
    tertiaryContainer = OnEmblemGoldContainer,
    onTertiaryContainer = EmblemGoldContainer,
    background = SurfaceDarkBg,
    onBackground = Color(0xFFE2E8F0),
    surface = SurfaceCardDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E3252),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = SaiphaiNavy,
    onPrimary = Color.White,
    primaryContainer = EmblemGoldContainer,
    onPrimaryContainer = OnEmblemGoldContainer,
    secondary = AutoGreen,
    onSecondary = Color.White,
    secondaryContainer = AutoGreenContainer,
    onSecondaryContainer = OnAutoGreenContainer,
    tertiary = EmblemGoldDark,
    onTertiary = SaiphaiNavyDark,
    tertiaryContainer = EmblemGoldContainer,
    onTertiaryContainer = OnEmblemGoldContainer,
    background = SurfaceCream,
    onBackground = SaiphaiNavyDark,
    surface = SurfaceCardLight,
    onSurface = SaiphaiNavyDark,
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
