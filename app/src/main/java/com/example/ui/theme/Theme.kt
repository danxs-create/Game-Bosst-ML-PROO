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

private val DarkGamingColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyan.copy(alpha = 0.2f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurple.copy(alpha = 0.2f),
    onSecondaryContainer = NeonPurple,
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    error = NeonRed,
    onError = Color.White,
    errorContainer = NeonRed.copy(alpha = 0.2f),
    onErrorContainer = NeonRed,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for gaming vibe
  dynamicColor: Boolean = false, // Disable dynamic colors to keep neon glow
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkGamingColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
