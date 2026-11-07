package com.rocha.jancarlauncher.can

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutsideTempTest {
    private fun raw(highByte: Int, lowByte: Int) = (highByte shl 8) or lowByte

    @Test fun inteiroEmCelsius() = assertEquals(25f, decodeOutsideTempC(raw(125, 0x00))!!, 0.001f)

    @Test fun meioGrauPositivo() = assertEquals(29.5f, decodeOutsideTempC(raw(129, 0x80))!!, 0.001f)

    @Test fun meioGrauNegativo() = assertEquals(-5.5f, decodeOutsideTempC(raw(95, 0x80))!!, 0.001f)

    @Test fun zeroComMeioGrauSobe() = assertEquals(0.5f, decodeOutsideTempC(raw(100, 0x80))!!, 0.001f)

    @Test fun pontoTipo1NaoSomaMeioGrau() = assertEquals(20f, decodeOutsideTempC(raw(120, 0x40))!!, 0.001f)

    @Test fun fahrenheitConvertidoParaCelsius() =
        assertEquals(25f, decodeOutsideTempC(raw(77 + 60, 0x01))!!, 0.001f)

    @Test fun desconhecido() {
        assertNull(decodeOutsideTempC(null))
        assertNull(decodeOutsideTempC(255))
    }
}
