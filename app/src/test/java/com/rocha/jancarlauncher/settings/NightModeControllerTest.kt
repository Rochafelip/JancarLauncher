package com.rocha.jancarlauncher.settings

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class NightModeControllerTest {

    @Test
    fun `entrando na janela e ainda nao aplicado aplica a noite guardando o estado atual`() {
        val action = NightModeController.evaluate(
            now = LocalTime.of(20, 0),
            wasApplied = false,
            currentDark = false,
            currentBrightness = 80,
            previousDark = false,
            previousBrightness = null,
        )
        assertEquals(NightModeController.Action.ApplyNight(previousDark = false, previousBrightness = 80), action)
    }

    @Test
    fun `saindo da janela e aplicado restaura o estado anterior`() {
        val action = NightModeController.evaluate(
            now = LocalTime.of(5, 30),
            wasApplied = true,
            currentDark = true,
            currentBrightness = NightModePolicy.NIGHT_BRIGHTNESS,
            previousDark = false,
            previousBrightness = 80,
        )
        assertEquals(NightModeController.Action.RestoreDay(dark = false, brightness = 80), action)
    }

    @Test
    fun `dentro da janela e ja aplicado nao faz nada`() {
        val action = NightModeController.evaluate(
            now = LocalTime.of(23, 0),
            wasApplied = true,
            currentDark = true,
            currentBrightness = NightModePolicy.NIGHT_BRIGHTNESS,
            previousDark = false,
            previousBrightness = 80,
        )
        assertEquals(NightModeController.Action.NoOp, action)
    }

    @Test
    fun `fora da janela e nao aplicado nao faz nada`() {
        val action = NightModeController.evaluate(
            now = LocalTime.of(12, 0),
            wasApplied = false,
            currentDark = false,
            currentBrightness = 80,
            previousDark = false,
            previousBrightness = 80,
        )
        assertEquals(NightModeController.Action.NoOp, action)
    }
}
