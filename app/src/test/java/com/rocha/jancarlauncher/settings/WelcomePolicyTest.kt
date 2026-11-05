package com.rocha.jancarlauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomePolicyTest {
    @Test fun launch_firstEverRun_shows() = assertTrue(WelcomePolicy.shouldShowOnLaunch(bootNow = 7, bootSaved = null))

    @Test fun launch_sameBoot_doesNotShow() = assertFalse(WelcomePolicy.shouldShowOnLaunch(bootNow = 7, bootSaved = 7))

    @Test fun launch_newBoot_shows() = assertTrue(WelcomePolicy.shouldShowOnLaunch(bootNow = 8, bootSaved = 7))

    @Test fun wake_after5Minutes_doesNotShow() {
        assertFalse(WelcomePolicy.shouldShowOnWake(screenOffAtMs = 0, nowMs = 5 * 60_000L))
    }

    @Test fun wake_afterExactlyThreshold_shows() {
        assertTrue(WelcomePolicy.shouldShowOnWake(screenOffAtMs = 0, nowMs = WelcomePolicy.WAKE_THRESHOLD_MS))
    }

    @Test fun wake_after11Minutes_shows() {
        assertTrue(WelcomePolicy.shouldShowOnWake(screenOffAtMs = 1_000, nowMs = 1_000 + 11 * 60_000L))
    }

    @Test fun wake_withoutRecordedScreenOff_doesNotShow() {
        assertFalse(WelcomePolicy.shouldShowOnWake(screenOffAtMs = null, nowMs = 99_999_999L))
    }

    @Test fun wakeThreshold_isTenMinutes() = assertEquals(600_000L, WelcomePolicy.WAKE_THRESHOLD_MS)
}
