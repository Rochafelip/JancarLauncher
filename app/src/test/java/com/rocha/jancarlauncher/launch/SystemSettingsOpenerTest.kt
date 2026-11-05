package com.rocha.jancarlauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemSettingsOpenerTest {
    private class FakeStarter(private val failing: Set<String> = emptySet()) : SettingsStarter {
        val attempts = mutableListOf<String>()
        override fun start(action: String) {
            attempts += action
            if (action in failing) throw RuntimeException("sem tela para $action")
        }
    }

    @Test fun specificAction_opensDirectly() {
        val starter = FakeStarter()
        assertTrue(SystemSettingsOpener(starter).open(SettingsActions.WIFI))
        assertEquals(listOf(SettingsActions.WIFI), starter.attempts)
    }

    @Test fun specificActionMissing_fallsBackToGeneralSettings() {
        val starter = FakeStarter(failing = setOf(SettingsActions.BLUETOOTH))
        assertTrue(SystemSettingsOpener(starter).open(SettingsActions.BLUETOOTH))
        assertEquals(listOf(SettingsActions.BLUETOOTH, SettingsActions.MAIN), starter.attempts)
    }

    @Test fun everythingMissing_returnsFalse_withoutThrowing() {
        val starter = FakeStarter(failing = setOf(SettingsActions.LOCATION, SettingsActions.MAIN))
        assertFalse(SystemSettingsOpener(starter).open(SettingsActions.LOCATION))
    }

    @Test fun mainSettingsFailing_isTriedOnlyOnce() {
        val starter = FakeStarter(failing = setOf(SettingsActions.MAIN))
        assertFalse(SystemSettingsOpener(starter).open(SettingsActions.MAIN))
        assertEquals(listOf(SettingsActions.MAIN), starter.attempts)
    }
}
