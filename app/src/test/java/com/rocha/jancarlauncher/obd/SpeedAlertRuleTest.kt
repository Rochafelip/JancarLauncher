package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedAlertRuleTest {
    /** Velocidade constante de [from] a [to] ms, um dado a cada 100 ms; devolve os bipes com o instante. */
    private fun SpeedAlertRule.run(from: Long, to: Long, kmh: Float?, limit: Int? = 110): List<Pair<Long, Beep>> {
        val out = mutableListOf<Pair<Long, Beep>>()
        var t = from
        while (t <= to) { update(kmh, limit, t)?.let { out += t to it }; t += 100 }
        return out
    }

    @Test fun over2kmh_for2s_beepsTwice_thenOnceEvery20s() {
        val r = SpeedAlertRule()
        assertEquals(emptyList<Pair<Long, Beep>>(), r.run(0, 1_900, 112f))
        val beeps = r.run(2_000, 42_000, 112f)
        assertEquals(listOf(2_000L to Beep.Double, 22_000L to Beep.Single, 42_000L to Beep.Single), beeps)
    }

    @Test fun smallDrop_doesNotRearm() {
        val r = SpeedAlertRule()
        r.run(0, 2_000, 112f)
        r.run(2_100, 5_000, 106f)
        val beeps = r.run(5_100, 23_000, 112f)
        assertEquals(listOf(22_000L to Beep.Single), beeps)
    }

    @Test fun dropBelowLimitMinus5_rearms() {
        val r = SpeedAlertRule()
        r.run(0, 2_000, 112f)
        r.run(2_100, 5_000, 104f)
        assertEquals(listOf(7_100L to Beep.Double), r.run(5_100, 8_000, 112f))
    }

    @Test fun noLimit_neverBeeps_andNoSpeedResetsTimer() {
        assertEquals(emptyList<Pair<Long, Beep>>(), SpeedAlertRule().run(0, 30_000, 150f, limit = null))
        val r = SpeedAlertRule()
        r.run(0, 1_500, 112f)
        assertNull(r.update(null, 110, 1_600))
        assertEquals(emptyList<Pair<Long, Beep>>(), r.run(1_700, 3_000, 112f))
    }

    @Test fun over_isLimitPlus2() {
        assertTrue(SpeedAlertRule.over(112f, 110))
        assertFalse(SpeedAlertRule.over(111f, 110))
        assertFalse(SpeedAlertRule.over(150f, null))
        assertFalse(SpeedAlertRule.over(null, 110))
    }

    @Test fun stepLimit_40to150_by10() {
        assertEquals(40, stepLimit(null, +10))
        assertNull(stepLimit(40, -10))
        assertEquals(150, stepLimit(150, +10))
        assertEquals(120, stepLimit(110, +10))
        assertNull(stepLimit(null, -10))
    }
}
