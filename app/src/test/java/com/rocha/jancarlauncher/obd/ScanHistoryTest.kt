package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScanHistoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun scan(t: Long) = ScanResult(t, MilStatus(true, 1), listOf("P0301"), emptyList(), null)

    @Test fun keepsNewest10_newestFirst() {
        val h = ScanHistory(tmp.root)
        for (t in 1L..12L) h.add(scan(t))
        val list = h.list()
        assertEquals(10, list.size)
        assertEquals(12L, list.first().wallMs)
        assertEquals(3L, list.last().wallMs)
    }

    @Test fun roundTrip_keepsNullsAndError() {
        val h = ScanHistory(tmp.root)
        val r = ScanResult(5L, null, listOf("P0133", "P0420"), listOf("P0171"), null, error = "Não foi possível concluir a leitura")
        h.add(r)
        val back = ScanHistory(tmp.root).list().single()
        assertEquals(r, back)
        assertNull(back.permanent)
        assertNull(back.mil)
    }

    @Test fun corruptFile_isEmpty_andNextAddRewrites() {
        File(tmp.root, "scans.json").writeText("{lixo")
        val h = ScanHistory(tmp.root)
        assertEquals(emptyList<ScanResult>(), h.list())
        h.add(scan(1L))
        assertEquals(1, h.list().size)
    }
}
