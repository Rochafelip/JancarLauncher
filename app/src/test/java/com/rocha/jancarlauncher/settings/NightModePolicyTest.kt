package com.rocha.jancarlauncher.settings

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class NightModePolicyTest {

    @Test
    fun `antes das 20h00 nao e noite`() {
        assertEquals(false, NightModePolicy.isNight(LocalTime.of(19, 59)))
    }

    @Test
    fun `as 20h00 e noite`() {
        assertEquals(true, NightModePolicy.isNight(LocalTime.of(20, 0)))
    }

    @Test
    fun `as 23h59 e noite`() {
        assertEquals(true, NightModePolicy.isNight(LocalTime.of(23, 59)))
    }

    @Test
    fun `a meia-noite e noite`() {
        assertEquals(true, NightModePolicy.isNight(LocalTime.of(0, 0)))
    }

    @Test
    fun `as 05h29 ainda e noite`() {
        assertEquals(true, NightModePolicy.isNight(LocalTime.of(5, 29)))
    }

    @Test
    fun `as 05h30 ja nao e noite`() {
        assertEquals(false, NightModePolicy.isNight(LocalTime.of(5, 30)))
    }

    @Test
    fun `meio-dia nao e noite`() {
        assertEquals(false, NightModePolicy.isNight(LocalTime.of(12, 0)))
    }
}
