package com.muzaffer.bistai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightBg = Color(0xFFFFFFFF)
val LightSurface1 = Color(0xFFF5F5F5)
val LightSurface2 = Color(0xFFEBEBEB)
val LightDivider = Color(0xFFEDEDED)
val LightTextPrimary = Color(0xFF0A0A0A)
val LightTextSecondary = Color(0xFF6B6B6B)
val LightPositive = Color(0xFF0E9F6E)
val LightNegative = Color(0xFFE5484D)

val DarkBg = Color(0xFF000000)
val DarkSurface1 = Color(0xFF141414)
val DarkSurface2 = Color(0xFF262626)
val DarkDivider = Color(0xFF1F1F1F)
val DarkTextPrimary = Color(0xFFF2F2F2)
val DarkTextSecondary = Color(0xFF8A8A8A)
val DarkPositive = Color(0xFF3DD68C)
val DarkNegative = Color(0xFFFF6369)

val WarningAmber = Color(0xFFD97706)

data class BistaiPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val selectedPill: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val positive: Color,
    val negative: Color
)

val LightBistaiPalette = BistaiPalette(
    isDark = false,
    bg = LightBg,
    surface1 = LightSurface1,
    surface2 = LightSurface2,
    selectedPill = LightBg,
    divider = LightDivider,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    positive = LightPositive,
    negative = LightNegative
)

val DarkBistaiPalette = BistaiPalette(
    isDark = true,
    bg = DarkBg,
    surface1 = DarkSurface1,
    surface2 = DarkSurface2,
    selectedPill = DarkSurface2,
    divider = DarkDivider,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    positive = DarkPositive,
    negative = DarkNegative
)

val LocalBistaiPalette = staticCompositionLocalOf { DarkBistaiPalette }

val TrendUp: Color
    @Composable get() = LocalBistaiPalette.current.positive

val TrendDown: Color
    @Composable get() = LocalBistaiPalette.current.negative
