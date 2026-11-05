package com.rocha.jancarlauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class JancarColors(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val faint: Color,
    val surface: Color,
    val surfaceHover: Color,
    val surfaceBorder: Color,
    val surfaceHoverBorder: Color,
    val icon: Color,
    val accent: Color,
)

private val Accent = Color(0xFF6FA3D8)

val DarkColors = JancarColors(
    bg = Color(0xFF0A0B0F),
    fg = Color(0xFFEEF0F2),
    muted = Color(0xFF888D94),
    faint = Color(0xFF53565C),
    surface = Color(0xFF15161B),
    surfaceHover = Color(0xFF1C1E24),
    surfaceBorder = Color(1f, 1f, 1f, 0.07f),
    surfaceHoverBorder = Color(1f, 1f, 1f, 0.13f),
    icon = Color(0xFFC9CDD3),
    accent = Accent,
)

val LightColors = JancarColors(
    bg = Color(0xFFF1F2F4),
    fg = Color(0xFF16181C),
    muted = Color(0xFF696D73),
    faint = Color(0xFF9A9DA3),
    surface = Color(0xFFFBFBFC),
    surfaceHover = Color(0xFFE9EAED),
    surfaceBorder = Color(16 / 255f, 18 / 255f, 22 / 255f, 0.08f),
    surfaceHoverBorder = Color(16 / 255f, 18 / 255f, 22 / 255f, 0.14f),
    icon = Color(0xFF3B3E44),
    accent = Accent,
)

val LocalJancarColors = staticCompositionLocalOf { DarkColors }
