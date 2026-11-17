package com.rocha.jancarlauncher.obd

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DtcCatalogTest {
    private val json = File("src/main/assets/obd/dtc_pt.json").readText()
    private val catalog = DtcCatalog(json)

    @Test fun realTable_isComplete() {
        val o = JSONObject(json)
        val codes = o.keys().asSequence().toList()
        assertTrue("só ${codes.size} códigos", codes.size >= 120)
        for (code in codes) {
            assertTrue(code, Regex("^P0[0-9A-F]{3}$").matches(code))
            val e = o.getJSONObject(code)
            assertTrue(code, e.getString("t").isNotBlank())
            assertTrue(code, e.getString("s") in setOf("baixa", "media", "alta"))
            assertTrue(code, e.getJSONArray("c").length() >= 1)
            assertTrue(code, e.getJSONArray("v").length() >= 1)
        }
    }

    @Test fun knownCode_hasSeverity() {
        val i = catalog.describe("P0301")
        assertTrue(i.known)
        assertEquals(Severity.Alta, i.severity)
        assertTrue(i.causes.isNotEmpty() && i.checks.isNotEmpty())
    }

    @Test fun unknownGeneric_andManufacturer() {
        val g = catalog.describe("P0999")
        assertFalse(g.known)
        assertEquals("Código genérico sem descrição nesta versão", g.title)
        assertTrue(catalog.describe("P1234").title.contains("fabricante"))
        assertFalse(catalog.describe("U0100").known)
    }
}
