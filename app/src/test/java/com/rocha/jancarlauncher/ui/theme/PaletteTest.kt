package com.rocha.jancarlauncher.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    @Test fun isLight_survivesTranslucentSurfaces() {
        // Sobre a foto as telas usam a superfície translúcida: continua sendo o tema claro.
        assertTrue(LightColors.copy(surface = LightColors.surface.copy(alpha = 0.84f)).isLight)
        assertTrue(LightColors.isLight)
        assertFalse(DarkColors.isLight)
        assertFalse(DarkColors.copy(surface = DarkColors.surface.copy(alpha = 0.84f)).isLight)
    }
}
