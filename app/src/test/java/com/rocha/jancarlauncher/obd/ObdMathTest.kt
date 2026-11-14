package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElmProtocolTest {
    @Test fun request_isMode01WithTwoHexDigits() {
        assertEquals("010C", ElmProtocol.request(ElmProtocol.RPM))
        assertEquals("0152", ElmProtocol.request(ElmProtocol.ETHANOL))
    }

    @Test fun parse_withOrWithoutSpacesAndPrompt() {
        assertArrayEquals(intArrayOf(0x1A, 0xF8), ElmProtocol.parse("41 0C 1A F8\r\r>", 0x0C, 2))
        assertArrayEquals(intArrayOf(0x1A, 0xF8), ElmProtocol.parse("410C1AF8>", 0x0C, 2))
        assertArrayEquals(intArrayOf(0x32), ElmProtocol.parse("SEARCHING...\r410D32\r>", 0x0D, 1))
    }

    @Test fun parse_noDataOrShortAnswer_isNull() {
        assertNull(ElmProtocol.parse("NO DATA\r>", 0x10, 2))
        assertNull(ElmProtocol.parse("?\r>", 0x10, 2))
        assertNull(ElmProtocol.parse("4110 1A>", 0x10, 2))
        assertNull(ElmProtocol.parse("410D32>", 0x0C, 2)) // resposta de outro PID
    }

    @Test fun decoders_followSaeJ1979() {
        assertEquals(1726f, ElmProtocol.rpm(intArrayOf(0x1A, 0xF8)), 0.01f)
        assertEquals(50f, ElmProtocol.speed(intArrayOf(0x32)), 0f)
        assertEquals(25f, ElmProtocol.intakeTemp(intArrayOf(65)), 0f)
        assertEquals(90f, ElmProtocol.coolantTemp(intArrayOf(130)), 0f)
        assertEquals(6.5f, ElmProtocol.mafGramsPerSecond(intArrayOf(0x02, 0x8A)), 0.001f)
        assertEquals(100f, ElmProtocol.ethanolPercent(intArrayOf(255)), 0.01f)
    }

    @Test fun supported_readsTheBitmap() {
        // BE 1F A8 13: suporta 01, 03-06, 0C-10 (inclui RPM, velocidade, IAT, MAF), 13, 15, 1C, 1F, 20.
        val s = ElmProtocol.supported(intArrayOf(0xBE, 0x1F, 0xA8, 0x13), 0x00)
        assertTrue(ElmProtocol.RPM in s && ElmProtocol.SPEED in s && ElmProtocol.IAT in s && ElmProtocol.MAF in s)
        assertTrue(ElmProtocol.MAP !in s)
        assertTrue(0x20 in s)
    }
}

class FuelModelTest {
    @Test fun idleOfA1liter_isAboutTwoGramsOfAirAndUnderOneLiterPerHour() {
        val air = FuelModel.airFromManifold(mapKpa = 30f, intakeC = 30f, rpm = 850f)
        assertEquals(1.95f, air, 0.1f)
        val lph = FuelModel.litersPerHour(air, FuelModel.Blend.Gasolina.ethanol)
        assertTrue("marcha lenta de um 1.0 gasta menos de 1 L/h: $lph", lph in 0.5f..0.9f)
    }

    @Test fun ethanolSpendsMoreThanGasoline() {
        val gas = FuelModel.litersPerHour(8f, FuelModel.Blend.Gasolina.ethanol)
        val eth = FuelModel.litersPerHour(8f, FuelModel.Blend.Etanol.ethanol)
        assertTrue(eth > gas * 1.3f)
    }

    @Test fun kmPerLiter_isNullWhenStopped() {
        assertNull(FuelModel.kmPerLiter(0f, 0.7f))
        assertEquals(20f, FuelModel.kmPerLiter(60f, 3f)!!, 0.001f)
    }

    @Test fun blend_cyclesAndDefaultsToGasoline() {
        assertEquals(FuelModel.Blend.Mistura, FuelModel.Blend.Gasolina.next())
        assertEquals(FuelModel.Blend.Gasolina, FuelModel.Blend.Etanol.next())
        assertEquals(FuelModel.Blend.Gasolina, FuelModel.Blend.fromName(null))
    }
}

class TripTest {
    @Test fun accumulatesLitersAndKm() {
        var t = Trip()
        repeat(3600) { t = t.add(litersPerHour = 4f, speedKmh = 60f, dtSeconds = 1.0) }
        assertEquals(4.0, t.liters, 1e-6)
        assertEquals(60.0, t.km, 1e-6)
        assertEquals(15.0, t.kmPerLiter!!, 1e-6)
    }

    @Test fun gapsAreIgnored_carTurnedOffInTheMiddle() {
        val t = Trip().add(4f, 60f, 120.0)
        assertEquals(0.0, t.km, 0.0)
    }

    @Test fun noAverageUntilThereIsEnoughData() {
        assertNull(Trip(liters = 0.01, km = 0.1).kmPerLiter)
    }
}
