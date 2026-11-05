package com.rocha.jancarlauncher.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var store: SettingsStore

    @Before fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val ds = PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("settings.preferences_pb").also { it.delete() }
        }
        store = SettingsStore(ds)
    }

    @After fun tearDown() { scope.cancel() }

    @Test fun darkTheme_defaultsToTrue() = runBlocking {
        assertTrue(store.dark.first())
    }

    @Test fun darkTheme_persistsLightChoice() = runBlocking {
        store.setDark(false)
        assertFalse(store.dark.first())
    }

    @Test fun darkTheme_canReturnToDark() = runBlocking {
        store.setDark(false)
        store.setDark(true)
        assertTrue(store.dark.first())
    }

    @Test fun userBrightness_isNullUntilUserChoosesOne() = runBlocking {
        // Sem escolha do usuário o launcher não mexe no brilho da central.
        assertNull(store.userBrightness.first())
    }

    @Test fun userBrightness_persistsValue() = runBlocking {
        store.setUserBrightness(35)
        assertEquals(35, store.userBrightness.first())
    }

    @Test fun lastBootCount_isNullUntilSet() = runBlocking {
        assertNull(store.lastBootCount.first())
        store.setLastBootCount(12)
        assertEquals(12, store.lastBootCount.first())
    }

    @Test fun nightMode_defaultsToNotApplied() = runBlocking {
        assertFalse(store.nightModeApplied.first())
    }

    @Test fun nightMode_applyGuardsPreviousStateAndSwitchesToNight() = runBlocking {
        store.setDark(false)
        store.setUserBrightness(80)
        store.applyNightMode(previousDark = false, previousBrightness = 80)

        assertTrue(store.nightModeApplied.first())
        assertTrue(store.dark.first())
        assertEquals(NightModePolicy.NIGHT_BRIGHTNESS, store.userBrightness.first())
        assertFalse(store.preNightDark.first())
        assertEquals(80, store.preNightBrightness.first())
    }

    @Test fun nightMode_applyNeverTouchesBrightnessTheUserNeverChose() = runBlocking {
        store.applyNightMode(previousDark = false, previousBrightness = null)
        assertNull(store.userBrightness.first())
    }

    @Test fun nightMode_restoreBringsBackThePreviousState() = runBlocking {
        store.applyNightMode(previousDark = false, previousBrightness = 80)
        store.restoreDayMode(dark = false, brightness = 80)

        assertFalse(store.nightModeApplied.first())
        assertFalse(store.dark.first())
        assertEquals(80, store.userBrightness.first())
    }
}
