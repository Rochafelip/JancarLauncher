package com.rocha.jancarlauncher.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiScaleTest {
    @Test fun centralDe1024x600NaoMudaAEscalaAutomatica() {
        assertEquals(1f, UiScale.auto(1024f, 600f), 0.001f)
        assertEquals(1f, UiScale.auto(1024f, 534f), 0.001f)
    }

    @Test fun telaMaiorCresceETelaMenorEncolhe() {
        assertTrue(UiScale.auto(1280f, 720f) > 1.2f)
        assertTrue(UiScale.auto(800f, 480f) < 0.9f)
    }

    @Test fun escalaTemLimites() {
        assertEquals(UiScale.AUTO_MAX, UiScale.auto(4000f, 3000f), 0.001f)
        assertEquals(UiScale.AUTO_MIN, UiScale.auto(300f, 200f), 0.001f)
    }

    @Test fun tamanhoDoUsuarioMultiplicaQuandoCabe() {
        assertEquals(1.12f, UiScale.total(1024f, 600f, UiScale.Size.Grande), 0.001f)
    }

    @Test fun extraNaoPassaDoQueAindaCabe() {
        // 534 dp de altura / 1,3 = 410: ainda cabe; numa tela mais baixa o fator é limitado.
        val t = UiScale.total(1024f, 480f, UiScale.Size.Extra)
        assertTrue(480f / t >= UiScale.MIN_FIT_H - 0.5f)
    }

    @Test fun nomeInvalidoVoltaAoPadrao() {
        assertEquals(UiScale.Size.Default, UiScale.Size.fromName(null))
        assertEquals(UiScale.Size.Extra, UiScale.Size.fromName("Extra"))
    }
}
