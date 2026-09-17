package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GapooOrangeLight,
    onPrimary = Color.Black,
    primaryContainer = GapooOrangePrimary,
    onPrimaryContainer = Color.White,
    secondary = GapooTealSecondary,
    tertiary = GapooAmberTertiary,
    background = Color(0xFF191614),
    surface = Color(0xFF23201D),
    surfaceVariant = Color(0xFF332E2A),
    onBackground = Color(0xFFEDE0D4),
    onSurface = Color(0xFFEDE0D4)
)

private val LightColorScheme = lightColorScheme(
    primary = GapooOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = GapooOrangeContainer,
    onPrimaryContainer = GapooOnOrangeContainer,
    secondary = GapooTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = GapooTealContainer,
    onSecondaryContainer = GapooOnTealContainer,
    tertiary = GapooAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GapooAmberContainer,
    background = GapooBackgroundLight,
    surface = GapooSurfaceLight,
    surfaceVariant = GapooSurfaceVariant,
    onBackground = GapooTextPrimary,
    onSurface = GapooTextPrimary,
    onSurfaceVariant = GapooTextSecondary
)

@Composable
fun GapooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for backwards compatibility with starter template tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = GapooTheme(darkTheme, dynamicColor, content)
