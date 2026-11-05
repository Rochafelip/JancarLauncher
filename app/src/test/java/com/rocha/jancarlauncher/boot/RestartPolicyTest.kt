package com.rocha.jancarlauncher.boot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestartPolicyTest {
    private class MemoryLog(var times: List<Long> = emptyList()) : RestartLog {
        override fun read() = times
        override fun write(times: List<Long>) { this.times = times }
    }

    @Test fun firstThreeRestartsAreAllowed_fourthIsDenied() {
        val policy = RestartPolicy(MemoryLog())
        assertTrue(policy.allow(1_000))
        assertTrue(policy.allow(2_000))
        assertTrue(policy.allow(3_000))
        assertFalse(policy.allow(4_000))
    }

    @Test fun afterTheWindowPasses_restartsAreAllowedAgain() {
        val policy = RestartPolicy(MemoryLog())
        repeat(3) { assertTrue(policy.allow(1_000L + it)) }
        assertFalse(policy.allow(30_000))
        assertTrue(policy.allow(1_000L + 60_000)) // o mais antigo saiu da janela de 60 s
    }

    @Test fun deniedAttempt_doesNotExtendTheBlock() {
        val log = MemoryLog()
        val policy = RestartPolicy(log)
        repeat(3) { policy.allow(1_000L + it) }
        repeat(10) { assertFalse(policy.allow(2_000L + it)) }
        assertEquals(3, log.times.size) // negados não entram no histórico
        assertTrue(policy.allow(62_000)) // e por isso o bloqueio acaba junto com a janela original
    }

    @Test fun limitSurvivesProcessDeath_becauseHistoryIsInTheLog() {
        val sharedLog = MemoryLog()
        repeat(3) { assertTrue(RestartPolicy(sharedLog).allow(1_000L + it)) } // 3 "processos" diferentes
        assertFalse(RestartPolicy(sharedLog).allow(5_000)) // o 4º processo já nasce bloqueado
    }

    @Test fun clockJumpingBackwards_doesNotBlockForever() {
        val log = MemoryLog(listOf(10_000_000L, 10_000_001L, 10_000_002L)) // registros "no futuro" (RTC mudou)
        assertTrue(RestartPolicy(log).allow(1_000))
    }
}
