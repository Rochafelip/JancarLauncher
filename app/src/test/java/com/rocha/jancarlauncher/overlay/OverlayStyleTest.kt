package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.ui.home.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStyleTest {
    @Test fun todoTemaDaHomeTemUmEstilo() {
        // when exaustivo no código: este teste garante que nenhum tema cai fora e que os 3 desenhos são usados.
        val styles = HomeLayout.entries.map { OverlayStyle.of(it) }
        assertEquals(HomeLayout.entries.size, styles.size)
        assertEquals(OverlayStyle.entries.toSet(), styles.toSet())
    }

    @Test fun temaAtualContinuaNoCartaoDeSempre() {
        assertEquals(OverlayStyle.Cartao, OverlayStyle.of(HomeLayout.Atual))
    }

    @Test fun nomeInvalidoNaoTemEstilo() {
        assertEquals(null, OverlayStyle.fromName("xyz"))
        assertTrue(OverlayStyle.fromName("Barra") == OverlayStyle.Barra)
    }
}
