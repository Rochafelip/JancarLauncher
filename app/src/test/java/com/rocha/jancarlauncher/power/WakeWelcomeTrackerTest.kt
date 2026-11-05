package com.rocha.jancarlauncher.power

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWelcomeTrackerTest {
    private class MemoryStore(var value: Long? = null) : ScreenOffStore {
        override fun read() = value
        override fun write(ms: Long?) { value = ms }
    }

    private var clock = 0L
    private val minute = 60_000L

    private fun tracker(store: ScreenOffStore) = WakeWelcomeTracker(store, now = { clock })

    @Test fun screenOnAfterLongOff_showsWelcome_once() {
        val t = tracker(MemoryStore())
        clock = 0; t.onScreenOff()
        clock = 11 * minute
        assertTrue(t.onScreenOn())
        assertFalse(t.onScreenOn()) // o registro foi consumido
    }

    @Test fun screenOnAfterShortOff_doesNotShow() {
        val t = tracker(MemoryStore())
        clock = 0; t.onScreenOff()
        clock = 5 * minute
        assertFalse(t.onScreenOn())
    }

    @Test fun screenOnWithoutRecordedOff_doesNotShow() = assertFalse(tracker(MemoryStore()).onScreenOn())

    @Test fun survivesProcessDeath_becauseOffTimeIsInTheStore() {
        val store = MemoryStore()
        clock = 0; tracker(store).onScreenOff()          // processo A grava e morre durante a suspensão
        clock = 8 * 60 * minute                          // 8 h depois
        assertTrue(tracker(store).onScreenOn())          // processo B (novo) ainda decide certo
    }

    @Test fun launchAfterLongOff_withoutScreenOnEvent_showsWelcomeOnce() {
        val store = MemoryStore()
        clock = 0; tracker(store).onScreenOff()
        clock = 30 * minute                              // o SCREEN_ON passou antes de o app existir
        assertTrue(tracker(store).onLaunch())
        assertFalse(tracker(store).onLaunch())
    }

    @Test fun launchWithoutRecord_doesNotShow() = assertFalse(tracker(MemoryStore()).onLaunch())

    @Test fun screenOnConsumesRecord_soLaterLaunchDoesNotRepeatWelcome() {
        val t = tracker(MemoryStore())
        clock = 0; t.onScreenOff()
        clock = 20 * minute; assertTrue(t.onScreenOn())
        clock = 21 * minute; assertFalse(t.onLaunch())
    }

    @Test fun clockReset_afterReboot_doesNotShowFromStaleRecord() {
        val store = MemoryStore(value = 10_000_000L)     // registro de antes do reboot
        clock = 500                                      // elapsedRealtime recomeçou do zero
        assertFalse(tracker(store).onLaunch())
        assertEquals(null, store.value)
    }
}
