package com.rocha.jancarlauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test fun twelveThemes_lettersAtoL() {
        assertEquals(12, HomeLayout.entries.size)
        assertEquals("ABCDEFGHIJKL".map { it.toString() }, HomeLayout.entries.map { it.letter })
    }

    @Test fun savedName_orDefaultToA() {
        assertEquals(HomeLayout.Dock, HomeLayout.fromName("Dock"))
        assertEquals(HomeLayout.Atual, HomeLayout.fromName(null))
        assertEquals(HomeLayout.Atual, HomeLayout.fromName("Removido"))
    }

    @Test fun previews_stayInsideTheThumbnail() {
        for (l in HomeLayout.entries) for (b in l.preview) {
            assertTrue("${l.letter}: $b", b.x >= 0f && b.y >= 0f && b.x + b.w <= 1.001f && b.y + b.h <= 1.001f)
        }
    }

    @Test fun veils_areOrderedAndValid() {
        for (l in HomeLayout.entries) {
            val v = l.veil()
            assertEquals(0f, v.first().first, 0f)
            assertEquals(1f, v.last().first, 0f)
            assertTrue(v.zipWithNext().all { (a, b) -> a.first < b.first })
            assertTrue(v.all { it.second in 0f..1f })
        }
    }
}
