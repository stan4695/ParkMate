package com.parkmate.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ParkBlue,
    onPrimary = Color.White,
    primaryContainer = ParkBlueSoft,
    onPrimaryContainer = ParkBlue,
    secondary = ParkAmber,
    onSecondary = Color.White,
    secondaryContainer = ParkAmberSoft,
    onSecondaryContainer = ParkAmberText,
    background = ParkBgLight,
    onBackground = ParkTextLight,
    surface = ParkSurfaceLight,
    onSurface = ParkTextLight,
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = ParkMutedLight,
    outline = ParkLineLight,
    error = ParkDanger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = ParkBlueDark,
    onPrimary = Color(0xFF0A142E),
    primaryContainer = ParkBlueSoftDark,
    onPrimaryContainer = ParkBlueDark,
    secondary = ParkAmberDark,
    onSecondary = Color(0xFF281900),
    secondaryContainer = ParkAmberSoftDark,
    onSecondaryContainer = ParkAmberTextDark,
    background = ParkBgDark,
    onBackground = ParkTextDark,
    surface = ParkSurfaceDark,
    onSurface = ParkTextDark,
    surfaceVariant = Color(0xFF232834),
    onSurfaceVariant = ParkMutedDark,
    outline = ParkLineDark,
    error = Color(0xFFEF6B6B),
    onError = Color(0xFF3B0909)
)

@Composable
fun ParkMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalParkMateColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ParkMateTypography,
            shapes = ParkMateShapes,
            content = content
        )
    }
}

object ParkMateThemeTokens {
    val colors: ParkMateExtendedColors
        @Composable
        get() = LocalParkMateColors.current
}
