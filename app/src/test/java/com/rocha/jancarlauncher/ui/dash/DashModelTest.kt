package com.rocha.jancarlauncher.ui.dash

import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashModelTest {
    // --- barra de RPM ---

    @Test fun litSegments_unknownRpm_isZero() {
        assertEquals(0, DashModel.litSegments(null))
    }

    @Test fun litSegments_engineOff_isZero() {
        assertEquals(0, DashModel.litSegments(0f))
    }

    @Test fun litSegments_halfScale_lightsHalfTheBar() {
        assertEquals(DashModel.SEGMENTS / 2, DashModel.litSegments(DashModel.RPM_MAX / 2f))
    }

    @Test fun litSegments_atOrAboveMax_isFullBar_neverOverflows() {
        assertEquals(DashModel.SEGMENTS, DashModel.litSegments(DashModel.RPM_MAX.toFloat()))
        assertEquals(DashModel.SEGMENTS, DashModel.litSegments(DashModel.RPM_MAX * 3f))
    }

    @Test fun litSegments_idle_lightsAtLeastOneSegment() {
        // Marcha lenta (~850 rpm) precisa aparecer na barra, não ficar apagada.
        assertTrue(DashModel.litSegments(850f) >= 1)
    }

    @Test fun zone_lowSegments_areNormal() {
        assertEquals(DashModel.Zone.Normal, DashModel.zone(0))
    }

    @Test fun zone_segmentStartingAtAmberRpm_isWarn() {
        assertEquals(DashModel.Zone.Warn, DashModel.zone(DashModel.segmentFor(DashModel.RPM_WARN)))
    }

    @Test fun zone_segmentJustBeforeAmber_isNormal() {
        assertEquals(DashModel.Zone.Normal, DashModel.zone(DashModel.segmentFor(DashModel.RPM_WARN) - 1))
    }

    @Test fun zone_segmentStartingAtRedRpm_isRed_andLastSegmentIsRed() {
        assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.segmentFor(DashModel.RPM_RED)))
        assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.SEGMENTS - 1))
    }

    // --- aviso de troca ---

    @Test fun shiftLight_offBelowShiftPoint() {
        assertFalse(DashModel.shiftLight(DashModel.RPM_SHIFT - 1f))
        assertFalse(DashModel.shiftLight(null))
    }

    @Test fun shiftLight_onAtShiftPoint() {
        assertTrue(DashModel.shiftLight(DashModel.RPM_SHIFT.toFloat()))
    }

    // --- textos ---

    @Test fun speedText_unknown_isSingleDash() {
        assertEquals("—", DashModel.speedText(null))
    }

    @Test fun speedText_roundsToWholeKmh() {
        assertEquals("88", DashModel.speedText(87.6f))
        assertEquals("0", DashModel.speedText(0.2f))
    }

    @Test fun speedText_clampsToThreeDigits() {
        assertEquals("999", DashModel.speedText(1500f))
    }

    @Test fun rpmText_unknown_isSingleDash() {
        assertEquals("—", DashModel.rpmText(null))
    }

    @Test fun rpmText_roundsToTens_toAvoidFlicker() {
        assertEquals("3460", DashModel.rpmText(3456f))
        assertEquals("850", DashModel.rpmText(851f))
    }

    @Test fun odoText_unknown_isSingleDash() {
        assertEquals("—", DashModel.odoText(null))
    }

    @Test fun odoText_zeroOrNegative_isTreatedAsMissingData() {
        // Odômetro zerado/negativo num carro rodado = CAN sem dado, não "0 km".
        assertEquals("—", DashModel.odoText(0))
        assertEquals("—", DashModel.odoText(-1))
    }

    @Test fun odoText_usesThousandsSeparatorWithDot() {
        assertEquals("123.456", DashModel.odoText(123456))
        assertEquals("987", DashModel.odoText(987))
    }

    @Test fun valueText_oneDecimalWithComma_orSingleDash() {
        assertEquals("12,4", DashModel.valueText(12.38f))
        assertEquals("—", DashModel.valueText(null))
    }

    // --- origem dos dados: CAN primeiro, ICar de reserva ---

    private val noInt = Int.MIN_VALUE
    private val noFloat = Int.MIN_VALUE.toFloat()

    /** DashBoard como o CAN do Fox entrega: só odo/rpm/velocidade/combustível/temperatura preenchidos. */
    private fun canDash(odo: Int = noInt, rpm: Int = noInt, speed: Int = noInt, fuel: Float = noFloat, ambient: Float = noFloat) =
        DashBoard(
            gearShiftPosition = 0, odo = odo, cruisingRange = noInt, rotationRate = rpm, speed = speed,
            runningTime = noInt, fuelTankage = noInt, fuel = fuel, fuelPercentage = noFloat, avgFuelCont = noFloat,
            instFuelCont = noFloat, coolantTemp = noFloat, engineOilTemp = noFloat, inletTemp = noFloat,
            ambientTemp = ambient, accPedal = noInt, brakePedal = noInt, throttlePos = noInt,
        )

    @Test fun reading_usesCanValues_measuredOnTheFox() {
        // Leitura real na central (2026-11-07, motor em marcha lenta, tela original: 34,5 °C).
        val r = DashModel.reading(CarState(), canDash(odo = 68039, rpm = 873, fuel = 50f, ambient = 74.5f))
        assertEquals(873f, r.rpm!!, 0.001f)
        assertEquals(50f, r.fuelLiters!!, 0.001f)
        assertEquals(34.5f, r.outsideTempC!!, 0.001f)
        assertEquals(68039, r.odoKm)
    }

    @Test fun reading_canSpeedMissingWithLiveRpm_isZero() {
        // O app CAN descarta a velocidade 0 enquanto ela nunca mudou (CanPopWind.isSpeedValid).
        assertEquals(0f, DashModel.reading(CarState(), canDash(rpm = 873)).speed!!, 0.001f)
    }

    @Test fun reading_canSpeed_whenPresent() {
        assertEquals(62f, DashModel.reading(CarState(), canDash(rpm = 2500, speed = 62)).speed!!, 0.001f)
    }

    @Test fun reading_fallsBackToICar_whenCanHasNoData() {
        val car = CarState(speed = 40f, rpm = 2000f, fuelLiters = 30f, outsideTempRaw = (120 shl 8))
        val r = DashModel.reading(car, null)
        assertEquals(40f, r.speed!!, 0.001f)
        assertEquals(2000f, r.rpm!!, 0.001f)
        assertEquals(30f, r.fuelLiters!!, 0.001f)
        assertEquals(20f, r.outsideTempC!!, 0.001f)
        assertNull(r.odoKm)
    }

    @Test fun reading_missingEverywhere_isUnknown() {
        val r = DashModel.reading(CarState(), canDash())
        assertNull(r.speed)
        assertNull(r.rpm)
        assertNull(r.fuelLiters)
        assertNull(r.outsideTempC)
        assertNull(r.odoKm)
    }

    @Test fun reading_batteryOnlyFromICar_handbrakeUnavailable() {
        val r = DashModel.reading(CarState(batteryVoltage = 13.8f, handbrakeOn = false), canDash(rpm = 873))
        assertEquals(13.8f, r.batteryVoltage!!, 0.001f)
        // ICar entrega o fio do chicote (GPIO), não validado no Fox: não mostrar como fato.
        assertNull(r.handbrakeOn)
    }
}
