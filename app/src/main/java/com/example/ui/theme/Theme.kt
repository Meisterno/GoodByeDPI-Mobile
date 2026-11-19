package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFA3A3A3),
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFF222222),
    onTertiaryContainer = Color(0xFFD4D4D4),
    background = MonoBlackBg,
    onBackground = TextPrimaryDark,
    surface = MonoDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = MonoDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderOutlineDark,
    error = Color(0xFFB0B0B0),
    onError = Color(0xFF121212)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF141414),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF121212),
    secondary = Color(0xFF333333),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAEAEA),
    onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF595959),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0E0),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderOutlineLight,
    error = Color(0xFF404040),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
