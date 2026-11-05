package com.rocha.jancarlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    @Test
    fun darkPalette_hasExactHexValuesFromSpec() {
        assertEquals(Color(0xFF0A0B0F), DarkColors.bg)
        assertEquals(Color(0xFFEEF0F2), DarkColors.fg)
        assertEquals(Color(0xFF888D94), DarkColors.muted)
        assertEquals(Color(0xFF53565C), DarkColors.faint)
        assertEquals(Color(0xFF15161B), DarkColors.surface)
        assertEquals(Color(0xFF1C1E24), DarkColors.surfaceHover)
        assertEquals(Color(0xFFC9CDD3), DarkColors.icon)
    }

    @Test
    fun lightPalette_hasExactHexValuesFromSpec() {
        assertEquals(Color(0xFFF1F2F4), LightColors.bg)
        assertEquals(Color(0xFF16181C), LightColors.fg)
        assertEquals(Color(0xFF696D73), LightColors.muted)
        assertEquals(Color(0xFF9A9DA3), LightColors.faint)
        assertEquals(Color(0xFFFBFBFC), LightColors.surface)
        assertEquals(Color(0xFFE9EAED), LightColors.surfaceHover)
        assertEquals(Color(0xFF3B3E44), LightColors.icon)
    }

    @Test
    fun accent_isSteelBlueInBothThemes() {
        assertEquals(Color(0xFF6FA3D8), DarkColors.accent)
        assertEquals(Color(0xFF6FA3D8), LightColors.accent)
    }

    @Test
    fun borders_matchPreviewAlphas() {
        assertEquals(Color(1f, 1f, 1f, 0.07f), DarkColors.surfaceBorder)
        assertEquals(Color(1f, 1f, 1f, 0.13f), DarkColors.surfaceHoverBorder)
        assertEquals(Color(16 / 255f, 18 / 255f, 22 / 255f, 0.08f), LightColors.surfaceBorder)
        assertEquals(Color(16 / 255f, 18 / 255f, 22 / 255f, 0.14f), LightColors.surfaceHoverBorder)
    }
}
