package com.anitools.gab.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// AniTool Premium Theme by Gab — inspired by MangananoX, but cleaner
val DarkBg = Color(0xFF0F0F23) // Deep indigo
val DarkSurface = Color(0xFF1E1B4B)
val DarkCard = Color(0xFF2D2B6B)
val AccentViolet = Color(0xFF7C3AED) // Premium violet
val AccentIndigo = Color(0xFF4F46E5)
val AccentCyan = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF9CA3AF)

private val DarkColorScheme = darkColorScheme(
    primary = AccentViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E1A5C),
    onPrimaryContainer = AccentViolet,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = AccentIndigo,
    tertiary = AccentCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0F3A4B),
    onTertiaryContainer = AccentCyan,
    error = AccentRose,
    onError = Color.White,
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = AccentRose,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3B3A7A),
    outlineVariant = Color(0xFF2D2B6B),
)

@Composable
fun AniToolTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBg.toArgb()
            window.navigationBarColor = DarkBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// Backward compatibility for old color names (used in many screens)
val AccentGreen = AccentEmerald
val AccentBlue = AccentIndigo
val AccentPurple = AccentViolet
val AccentOrange = AccentAmber
val AccentRed = AccentRose
val TextMuted = TextSecondary
