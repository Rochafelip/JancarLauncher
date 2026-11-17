package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripCostTest {
    @Test fun ethanolWeight_mapsBlendTo0to1() {
        assertEquals(0.0, TripCost.ethanolWeight(0.27f), 1e-6)
        assertEquals(1.0, TripCost.ethanolWeight(1f), 1e-6)
        assertEquals(0.493, TripCost.ethanolWeight(0.63f), 0.001)
        assertEquals(0.0, TripCost.ethanolWeight(0f), 1e-6)
    }

    @Test fun reais_linearBetweenGasolineAndEthanol() {
        assertEquals(61.90, TripCost.reais(10.0, 0.0, 619, 429)!!, 1e-6)
        assertEquals(42.90, TripCost.reais(10.0, 10.0, 619, 429)!!, 1e-6)
        assertEquals(52.40, TripCost.reais(10.0, 5.0, 619, 429)!!, 1e-6)
    }

    @Test fun reais_missingPrice_isNull() {
        assertNull(TripCost.reais(10.0, 0.0, null, 429))
        assertNull(TripCost.reais(10.0, 0.0, 619, null))
    }

    @Test fun text() {
        assertEquals("R$ 52,40", TripCost.text(52.4))
        assertEquals("R$ — definir preço", TripCost.text(null))
    }

    @Test fun step_staysBetween1And20Reais() {
        assertEquals(629, TripCost.step(619, +10))
        assertEquals(100, TripCost.step(105, -10))
        assertEquals(2000, TripCost.step(1995, +10))
    }

    @Test fun tripAdd_tracksEthanolLiters() {
        var t = Trip()
        repeat(3600) { t = t.add(4f, 60f, 1.0, ethanol = 1f) }
        assertEquals(t.liters, t.ethanolLiters, 1e-9)
        var g = Trip()
        repeat(10) { g = g.add(4f, 60f, 1.0, ethanol = 0.27f) }
        assertEquals(0.0, g.ethanolLiters, 1e-9)
    }
}
