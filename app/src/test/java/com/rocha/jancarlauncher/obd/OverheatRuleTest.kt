package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverheatRuleTest {
    @Test fun hotFor10s_turnsOn() {
        val r = OverheatRule()
        r.update(111f, 0L)
        assertFalse(r.update(111f, 9_900L))
        assertTrue(r.update(111f, 10_000L))
    }

    @Test fun staysOnUntilBelow105() {
        val r = OverheatRule()
        r.update(111f, 0L)
        r.update(111f, 10_000L)
        assertTrue(r.update(106f, 11_000L))
        assertFalse(r.update(104f, 12_000L))
    }

    @Test fun noReading_isOff() {
        val r = OverheatRule()
        r.update(111f, 0L)
        assertFalse(r.update(null, 5_000L))
        assertFalse(r.update(111f, 12_000L))
    }
}
