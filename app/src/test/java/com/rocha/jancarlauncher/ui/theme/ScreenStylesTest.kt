package com.rocha.jancarlauncher.ui.theme

import com.rocha.jancarlauncher.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStylesTest {
    @Test fun defaults_areTheCurrentScreens() {
        val s = ScreenStyles.fromPrefs(emptyMap())
        assertEquals(ScreenStyles.Welcome.Centro, s.welcome)
        assertEquals(ScreenStyles.Drawer.Atual, s.drawer)
        assertEquals(ScreenStyles.Alert.Faixa, s.alert)
        assertEquals(ScreenStyles.Shift.Faixa, s.shift)
        assertEquals(ScreenStyles.Empty.Atual, s.empty)
        assertEquals(ScreenStyles.LightDash.Foto, s.lightDash)
    }

    @Test fun savedValues_andUnknownFallsBack() {
        val s = ScreenStyles.fromPrefs(mapOf("drawer" to "Grupos", "shift" to "Barra", "welcome" to "Removido"))
        assertEquals(ScreenStyles.Drawer.Grupos, s.drawer)
        assertEquals(ScreenStyles.Shift.Barra, s.shift)
        assertEquals(ScreenStyles.Welcome.Centro, s.welcome)
    }

    @Test fun everyScreenHasAKey_andPreviewsFitTheThumbnail() {
        assertEquals(6, SettingsStore.STYLE_KEYS.size)
        val all: List<ScreenStyles.Choice> = ScreenStyles.Welcome.entries + ScreenStyles.Drawer.entries + ScreenStyles.Alert.entries +
            ScreenStyles.Shift.entries + ScreenStyles.Empty.entries + ScreenStyles.LightDash.entries
        for (c in all) for (b in c.preview) {
            assertTrue("${c.label}: $b", b.x >= 0f && b.y >= 0f && b.x + b.w <= 1.001f && b.y + b.h <= 1.001f)
        }
    }
}
