package com.muzaffer.bistai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Color Schemes ────────────────────────────────────────────────────────────

private val BistaiDarkColors = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    secondary        = DarkSecondary,
    onSecondary      = DarkOnSecondary,
    background       = DarkBackground,
    onBackground     = DarkOnBackground,
    surface          = DarkSurface,
    onSurface        = DarkOnSurface,
    surfaceVariant   = DarkSurfaceVariant,
    outline          = DarkOutline,
    error            = BearishRed,
    onError          = White
)

private val BistaiLightColors = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary        = LightSecondary,
    onSecondary      = LightOnSecondary,
    background       = LightBackground,
    onBackground     = LightOnBackground,
    surface          = LightSurface,
    onSurface        = LightOnSurface,
    surfaceVariant   = LightSurfaceVariant,
    outline          = LightOutline,
    error            = BearishRed,
    onError          = White
)

// ─── BISTAITheme ──────────────────────────────────────────────────────────────

@Composable
fun BISTAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Marka kimliğini korumak için Dynamic Color kapalı
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BistaiDarkColors else BistaiLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = BistaiTypography,
        content     = content
    )
}
