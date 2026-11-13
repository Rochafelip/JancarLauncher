package com.rocha.jancarlauncher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiStatusTest {
    @Test fun quotesAreRemoved() {
        assertEquals("Casa 2.4G", WifiStatus.cleanSsid("\"Casa 2.4G\""))
    }

    @Test fun withoutPermission_theNameIsUnknown() {
        assertNull(WifiStatus.cleanSsid("<unknown ssid>"))
        assertNull(WifiStatus.cleanSsid("\"<unknown ssid>\""))
    }

    @Test fun emptyOrMissing_isUnknown() {
        assertNull(WifiStatus.cleanSsid(null))
        assertNull(WifiStatus.cleanSsid(""))
        assertNull(WifiStatus.cleanSsid("\"\""))
        assertNull(WifiStatus.cleanSsid("0x"))
    }

    @Test fun plainNameWithoutQuotes_isKept() {
        assertEquals("Fox", WifiStatus.cleanSsid("Fox"))
    }
}
