package com.rocha.jancarlauncher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun writesVersionStackAndLog() {
        val store = CrashStore(tmp.root)
        val file = store.write(1_000L, "1.8.0", RuntimeException("boom"), listOf("a", "b"))
        val text = file.readText()
        assertTrue(text.contains("1.8.0"))
        assertTrue(text.contains("RuntimeException: boom"))
        assertTrue(text.contains("a\n") && text.contains("b"))
        assertEquals("java.lang.RuntimeException: boom", store.list().single().firstLine)
    }

    @Test fun keepsNewest10_sortedNewestFirst() {
        val store = CrashStore(tmp.root)
        for (t in 1L..12L) store.write(t, "1.8.0", IllegalStateException("e$t"), emptyList())
        val list = store.list()
        assertEquals(10, list.size)
        assertEquals(12L, list.first().wallMs)
        assertEquals(3L, list.last().wallMs)
        assertTrue(store.read(list.first()).contains("e12"))
    }

    @Test fun clearRemovesAll() {
        val store = CrashStore(tmp.root)
        store.write(1L, "1.8.0", RuntimeException("x"), emptyList())
        store.clear()
        assertTrue(store.list().isEmpty())
    }
}
