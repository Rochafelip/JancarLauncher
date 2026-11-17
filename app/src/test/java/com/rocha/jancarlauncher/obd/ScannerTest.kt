package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ScannerTest {
    private class FakeLink(private val answers: Map<String, String>, private val failOn: String? = null) : ElmLink {
        override fun send(cmd: String, timeoutMs: Long): String {
            if (cmd == failOn) throw IOException("socket fechado")
            return answers[cmd] ?: "NO DATA"
        }
    }

    @Test fun readsMilConfirmedPendingPermanent() {
        val link = FakeLink(mapOf("0101" to "410183076500", "03" to "430201330301", "07" to "NO DATA", "0A" to "?"))
        val r = Scanner.run(link, 42L)
        assertEquals(42L, r.wallMs)
        assertEquals(MilStatus(true, 3), r.mil)
        assertEquals(listOf("P0133", "P0301"), r.confirmed)
        assertEquals(emptyList<String>(), r.pending)
        assertNull(r.permanent)
        assertNull(r.error)
    }

    @Test fun adapterDiesMidway_keepsWhatWasRead() {
        val link = FakeLink(mapOf("0101" to "410181076500", "03" to "430101330000"), failOn = "07")
        val r = Scanner.run(link, 1L)
        assertEquals("Não foi possível concluir a leitura", r.error)
        assertEquals(listOf("P0133"), r.confirmed)
    }

    @Test fun gate_onlyStoppedWithKnownSpeed() {
        assertFalse(ScanGate.allowed(null))
        assertTrue(ScanGate.allowed(2f))
        assertFalse(ScanGate.allowed(5f))
    }
}
