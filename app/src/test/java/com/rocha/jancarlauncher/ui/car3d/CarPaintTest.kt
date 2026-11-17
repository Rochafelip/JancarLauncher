package com.rocha.jancarlauncher.ui.car3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPaintTest {
    /** Aplica a matriz 4x5 (como o ColorMatrix do Android) a um pixel opaco, limitando a 0..255. */
    private fun apply(m: FloatArray, r: Int, g: Int, b: Int): IntArray = IntArray(3) { row ->
        val o = row * 5
        (m[o] * r + m[o + 1] * g + m[o + 2] * b + m[o + 3] * 255 + m[o + 4]).toInt().coerceIn(0, 255)
    }

    private val silver = Triple(200, 200, 200)
    private fun paint(p: CarPaint) = apply(p.matrix()!!, silver.first, silver.second, silver.third)

    @Test fun original_hasNoMatrix() {
        assertNull(CarPaint.Original.matrix())
    }

    @Test fun red_isRed() {
        val (r, g, b) = paint(CarPaint.Vermelho).let { Triple(it[0], it[1], it[2]) }
        assertTrue("r=$r g=$g", r > 1.8 * g && r > 1.8 * b)
    }

    @Test fun black_isDark_white_isLight() {
        assertTrue(paint(CarPaint.Preto).all { it < 90 })
        assertTrue(paint(CarPaint.Branco).all { it > 200 })
    }

    @Test fun allPaintsDiffer() {
        val results = CarPaint.entries.filter { it != CarPaint.Original }.map { paint(it).toList() }
        assertEquals(results.size, results.toSet().size)
    }

    @Test fun alphaIsKept() {
        val m = CarPaint.AzulMarinho.matrix()!!
        assertEquals(listOf(0f, 0f, 0f, 1f, 0f), m.slice(15..19))
    }

    @Test fun fromName_fallsBackToOriginal() {
        assertEquals(CarPaint.Original, CarPaint.fromName("x"))
        assertEquals(CarPaint.Vermelho, CarPaint.fromName("Vermelho"))
    }

    @Test fun noMask_noPaintRow() {
        assertFalse(paintAvailable(null))
    }
}
