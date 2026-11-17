package com.rocha.jancarlauncher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingLogTest {
    @Test fun keepsOnlyNewest200() {
        val log = RingLog()
        repeat(205) { log.add("obd", "msg$it", wallMs = 0L) }
        val lines = log.lines()
        assertEquals(200, lines.size)
        assertTrue(lines.first(), lines.first().endsWith("obd: msg5"))
        assertTrue(lines.last().endsWith("obd: msg204"))
    }
}
