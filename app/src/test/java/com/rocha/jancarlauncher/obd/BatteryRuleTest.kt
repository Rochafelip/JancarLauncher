package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryRuleTest {
    /** Alimenta a regra de segundo em segundo, de [from] a [to] s (inclusive); devolve o último resultado. */
    private fun BatteryRule.feed(from: Int, to: Int, volts: Float?, rpm: Float?): BatteryAlert? {
        var out: BatteryAlert? = null
        for (s in from..to) out = update(volts, rpm, s * 1000L)
        return out
    }

    @Test fun lowWithEngineRunning_after30s_for20s_isNotCharging() {
        val r = BatteryRule()
        r.feed(0, 30, 14.0f, 800f)
        assertNull(r.feed(31, 50, 12.4f, 800f))
        assertEquals(BatteryAlert.NotCharging, r.update(12.4f, 800f, 51_000L))
    }

    @Test fun notCharging_clearsOnlyAt13_3() {
        val r = BatteryRule()
        r.feed(0, 30, 14.0f, 800f)
        r.feed(31, 51, 12.4f, 800f)
        assertEquals(BatteryAlert.NotCharging, r.update(13.1f, 800f, 52_000L))
        assertNull(r.update(13.3f, 800f, 53_000L))
    }

    @Test fun highWithEngineRunning_for20s_isHighVoltage() {
        val r = BatteryRule()
        r.feed(0, 30, 14.0f, 800f)
        assertEquals(BatteryAlert.HighVoltage, r.feed(31, 51, 15.2f, 800f))
    }

    @Test fun lowWithEngineOff_for20s_isWeak() {
        val r = BatteryRule()
        assertNull(r.feed(0, 19, 12.0f, 0f))
        assertEquals(BatteryAlert.Weak, r.update(12.0f, 0f, 20_000L))
    }

    @Test fun crankingDip_isIgnored() {
        val r = BatteryRule()
        r.feed(0, 2, 12.6f, 0f)
        assertNull(r.feed(3, 9, 11.0f, 250f))
        assertNull(r.feed(10, 40, 14.1f, 850f))
    }

    @Test fun noVoltage_isNullAndResetsTimers() {
        val r = BatteryRule()
        r.feed(0, 15, 12.0f, 0f)
        assertNull(r.update(null, 0f, 16_000L))
        assertNull(r.feed(17, 30, 12.0f, 0f))
    }
}
