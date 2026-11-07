package com.rocha.jancarlauncher.power

import android.view.KeyEvent
import com.rocha.jancarlauncher.ui.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOffPolicyTest {
    @Test fun windowBrightness_off_isAbsoluteMinimum_evenWithUserChoice() {
        assertEquals(ScreenOffPolicy.WINDOW_BRIGHTNESS_OFF, ScreenOffPolicy.windowBrightness(Screen.Off, 80), 0f)
    }

    @Test fun windowBrightness_off_isAbsoluteMinimum_withoutUserChoice() {
        assertEquals(ScreenOffPolicy.WINDOW_BRIGHTNESS_OFF, ScreenOffPolicy.windowBrightness(Screen.Off, null), 0f)
    }

    @Test fun windowBrightness_off_bypassesSliderFloorOf10Percent() {
        assertTrue(ScreenOffPolicy.windowBrightness(Screen.Off, 10) < 0.1f)
    }

    @Test fun windowBrightness_awake_restoresUserChoice() {
        assertEquals(0.8f, ScreenOffPolicy.windowBrightness(Screen.Welcome, 80), 0.0001f)
    }

    @Test fun windowBrightness_awake_withoutUserChoice_followsSystem() {
        // Usuário nunca escolheu brilho: a janela não sobrescreve o brilho da central.
        assertEquals(ScreenOffPolicy.WINDOW_BRIGHTNESS_NONE, ScreenOffPolicy.windowBrightness(Screen.Home, null), 0f)
    }

    @Test fun brightnessChanges_areDeferredWhileOff() {
        // Ex.: modo noturno virando com a tela apagada não pode acender a tela.
        assertFalse(ScreenOffPolicy.shouldApplyBrightness(Screen.Off))
    }

    @Test fun brightnessChanges_applyWhenAwake() {
        assertTrue(ScreenOffPolicy.shouldApplyBrightness(Screen.Home))
        assertTrue(ScreenOffPolicy.shouldApplyBrightness(Screen.Welcome))
    }

    @Test fun wake_fromOff_goesToWelcome() {
        assertEquals(Screen.Welcome, ScreenOffPolicy.afterWake(Screen.Off))
    }

    @Test fun wake_whenNotOff_keepsScreen() {
        assertEquals(Screen.Drawer, ScreenOffPolicy.afterWake(Screen.Drawer))
    }

    @Test fun leavingForeground_whileOff_returnsToWelcome() {
        // Ex.: câmera de ré abriu por cima; ao voltar o launcher não fica preto.
        assertEquals(Screen.Welcome, ScreenOffPolicy.afterLeavingForeground(Screen.Off))
    }

    @Test fun volumeAndMediaKeys_passThroughWithoutWaking() {
        // Girar o volume com a tela apagada muda o volume e não acende a tela.
        listOf(
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STOP,
        ).forEach { assertFalse("keyCode $it", ScreenOffPolicy.keyWakes(it)) }
    }

    @Test fun otherKeys_wake() {
        listOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_DPAD_LEFT,
        ).forEach { assertTrue("keyCode $it", ScreenOffPolicy.keyWakes(it)) }
    }

    @Test fun leavingForeground_whenAwake_keepsScreen() {
        assertEquals(Screen.Home, ScreenOffPolicy.afterLeavingForeground(Screen.Home))
        assertEquals(Screen.Drawer, ScreenOffPolicy.afterLeavingForeground(Screen.Drawer))
    }
}
