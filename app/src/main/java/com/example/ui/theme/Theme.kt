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

// Sophisticated Dark Color Scheme
private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = DeepPurpleOnPrimary,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = LavenderOnContainer,
    secondary = LavenderOnContainer,
    onSecondary = DeepPurpleOnPrimary,
    secondaryContainer = SophisticatedSurfaceVariant,
    onSecondaryContainer = TextTertiary,
    tertiary = AmberGold,
    onTertiary = Color(0xFF381E72),
    error = ListeningCoral,
    onError = Color(0xFF690005),
    errorContainer = ListeningCoralContainer,
    onErrorContainer = ListeningCoral,
    background = SophisticatedBackground,
    onBackground = TextPrimary,
    surface = SophisticatedSurface,
    onSurface = TextPrimary,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SophisticatedOutline,
    outlineVariant = Color(0xFF3B383E)
)

private val LightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = DeepPurpleOnPrimary,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = LavenderOnContainer,
    secondary = LavenderOnContainer,
    onSecondary = DeepPurpleOnPrimary,
    background = SophisticatedBackground,
    onBackground = TextPrimary,
    surface = SophisticatedSurface,
    onSurface = TextPrimary,
    surfaceVariant = SophisticatedSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SophisticatedOutline
)

@Composable
fun VozLinguaTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SophisticatedDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
