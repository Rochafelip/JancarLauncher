package com.rocha.jancarlauncher.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLogRuleTest {
    @Test fun firstChange_andTypeChange_alwaysLog() {
        val r = MediaLogRule()
        assertTrue(r.shouldLog(type = 0, nowMs = 0L))
        assertTrue(r.shouldLog(type = 1, nowMs = 100L))
    }

    @Test fun radioSeek_sameTypeWithin30s_logsOnce() {
        val r = MediaLogRule()
        assertTrue(r.shouldLog(1, 0L))
        // Busca de estação: um passo de frequência a cada 50 ms durante 10 s.
        var logged = 0
        for (t in 50L..10_000L step 50L) if (r.shouldLog(1, t)) logged++
        assertTrue("logou $logged vezes", logged == 0)
    }

    @Test fun sameType_after30s_logsAgain() {
        val r = MediaLogRule()
        r.shouldLog(1, 0L)
        assertFalse(r.shouldLog(1, 29_999L))
        assertTrue(r.shouldLog(1, 30_000L))
    }
}
