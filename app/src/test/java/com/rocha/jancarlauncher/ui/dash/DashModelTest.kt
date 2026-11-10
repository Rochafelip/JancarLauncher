package com.rocha.jancarlauncher.ui.dash

import com.rocha.jancarlauncher.can.Cabin
import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import com.rocha.jancarlauncher.can.Lights
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

    // --- faixas da barra: seguem o ponto de troca do modo ---

    private val daily = DashModel.DriveMode.Daily
    private val sport = DashModel.DriveMode.Sport

    @Test fun driveModes_matchTheDriversShiftPoints() {
        // O usuário troca por volta de 3000 rpm no dia a dia e 5000 andando bravo.
        assertEquals(3000, daily.shiftRpm)
        assertEquals(5000, sport.shiftRpm)
    }

    @Test fun zone_lowSegments_areNormal() {
        assertEquals(DashModel.Zone.Normal, DashModel.zone(0, daily))
        assertEquals(DashModel.Zone.Normal, DashModel.zone(0, sport))
    }

    @Test fun zone_amberStartsHalfAThousandBeforeTheShiftPoint() {
        for (mode in DashModel.DriveMode.entries) {
            val warnStart = DashModel.segmentFor(mode.shiftRpm - DashModel.SHIFT_APPROACH_RPM)
            assertEquals(DashModel.Zone.Warn, DashModel.zone(warnStart, mode))
            assertEquals(DashModel.Zone.Normal, DashModel.zone(warnStart - 1, mode))
        }
    }

    @Test fun zone_redFromTheShiftPointToTheEnd() {
        for (mode in DashModel.DriveMode.entries) {
            assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.segmentFor(mode.shiftRpm), mode))
            assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.SEGMENTS - 1, mode))
        }
    }

    // --- aviso de troca ---

    @Test fun shiftStage_daily() {
        assertEquals(DashModel.ShiftStage.None, DashModel.shiftStage(null, daily))
        assertEquals(DashModel.ShiftStage.None, DashModel.shiftStage(2499f, daily))
        assertEquals(DashModel.ShiftStage.Near, DashModel.shiftStage(2500f, daily))
        assertEquals(DashModel.ShiftStage.Near, DashModel.shiftStage(2999f, daily))
        assertEquals(DashModel.ShiftStage.Shift, DashModel.shiftStage(3000f, daily))
    }

    @Test fun shiftStage_sport() {
        assertEquals(DashModel.ShiftStage.None, DashModel.shiftStage(4499f, sport))
        assertEquals(DashModel.ShiftStage.Near, DashModel.shiftStage(4500f, sport))
        assertEquals(DashModel.ShiftStage.Shift, DashModel.shiftStage(5000f, sport))
        assertEquals(DashModel.ShiftStage.Shift, DashModel.shiftStage(6500f, sport))
    }

    @Test fun driveMode_toggles() {
        assertEquals(sport, daily.next())
        assertEquals(daily, sport.next())
    }

    @Test fun driveMode_fromSavedName_unknownFallsBackToDaily() {
        assertEquals(sport, DashModel.DriveMode.fromName("Sport"))
        assertEquals(daily, DashModel.DriveMode.fromName(null))
        assertEquals(daily, DashModel.DriveMode.fromName("lixo"))
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

    // --- avisos: só aparecem quando algo acontece ---

    private fun reading(speed: Float? = 0f, fuel: Float? = 40f) =
        DashModel.Reading(speed = speed, rpm = 900f, fuelLiters = fuel, outsideTempC = 25f, odoKm = 68039)

    @Test fun alerts_allGood_isEmpty() {
        assertTrue(DashModel.alerts(reading()).isEmpty())
    }

    @Test fun alerts_lowFuel_atOrBelowReserve() {
        assertEquals(listOf(DashModel.Alert.LowFuel), DashModel.alerts(reading(fuel = DashModel.LOW_FUEL_LITERS)))
        assertTrue(DashModel.alerts(reading(fuel = DashModel.LOW_FUEL_LITERS + 1f)).isEmpty())
    }

    @Test fun alerts_fuelZeroOrUnknown_isNotAWarning() {
        // 0 L costuma ser o CAN ainda sem leitura logo após a partida, não tanque vazio.
        assertTrue(DashModel.alerts(reading(fuel = 0f)).isEmpty())
        assertTrue(DashModel.alerts(reading(fuel = null)).isEmpty())
    }

    // --- portas, setas e farol ---

    /** Cabin como o app CAN entrega: 1 = aberta, 0 = fechada, Int.MIN_VALUE = sem dado. */
    private fun canCabin(fl: Int = 0, fr: Int = 0, rl: Int = 0, rr: Int = 0, trunk: Int = 0, hood: Int = noInt) = Cabin(
        bonnetStatus = hood, flDoorStatus = fl, frDoorStatus = fr, rlDoorStatus = rl, rrDoorStatus = rr,
        trunkStatus = trunk, electricTrunk = noInt, electricTrunkDirection = noInt,
        leftSafetyBelt = 1, rightSafetyBelt = 1, sunroofStatus = noInt, sunroofSunshadeStatus = noInt,
        flWindowStatus = noInt, frWindowStatus = noInt, rlWindowStatus = noInt, rrWindowStatus = noInt,
        lockCarStatus = noInt, seatUpDownMoveStatus = noInt, seatFrontRearMoveStatus = noInt,
        seatHighLowMoveStatus = noInt, seatBackUpDownMoveStatus = noInt, seatBackFrontRearMoveStatus = noInt,
    )

    @Test fun reading_doorsFromCan_measuredOnTheFox() {
        // Leitura real (2026-11-07): porta do motorista aberta = flDoorStatus 1.
        val d = DashModel.reading(CarState(), null, canCabin(fl = 1)).doors
        assertEquals(true, d.frontLeft)
        assertEquals(false, d.frontRight)
        assertEquals(false, d.trunk)
        assertNull(d.hood) // o protocolo PQ não manda o capô
        assertTrue(d.anyOpen)
    }

    @Test fun reading_allClosed_noneOpen() {
        assertFalse(DashModel.reading(CarState(), null, canCabin()).doors.anyOpen)
    }

    @Test fun reading_doorsFallBackToICarMask() {
        // ICar: FRONT_RIGHT = bit 1, TRUNK = bit 6.
        val d = DashModel.reading(CarState(doorMask = (1 shl 1) or (1 shl 6)), null, null).doors
        assertEquals(true, d.frontRight)
        assertEquals(true, d.trunk)
        assertEquals(false, d.frontLeft)
    }

    @Test fun reading_noDoorData_isUnknown() {
        val d = DashModel.reading(CarState(), null, null).doors
        assertFalse(d.known)
        assertFalse(d.anyOpen)
    }

    @Test fun reading_turnSignals_hazardLightsBoth() {
        val r = DashModel.reading(CarState(), null, null, Lights(0, 0, 1))
        assertEquals(true, r.turnLeft)
        assertEquals(true, r.turnRight)
    }

    @Test fun reading_turnSignals_leftOnly() {
        val r = DashModel.reading(CarState(), null, null, Lights(1, 0, 0))
        assertEquals(true, r.turnLeft)
        assertEquals(false, r.turnRight)
    }

    @Test fun reading_turnSignals_neverReceived_isUnknown() {
        val r = DashModel.reading(CarState(), null, null, Lights(noInt, noInt, noInt))
        assertNull(r.turnLeft)
        assertNull(r.turnRight)
    }

    @Test fun reading_headlight_comesFromHarnessWire() {
        assertEquals(true, DashModel.reading(CarState(headlightOn = true), null).headlightOn)
        assertNull(DashModel.reading(CarState(), null).headlightOn)
    }

    @Test fun alerts_doorOpenWhileMoving_isRedAndFirst() {
        val r = reading(speed = 20f, fuel = 5f).copy(doors = DashModel.Doors(frontLeft = true))
        assertEquals(listOf(DashModel.Alert.DoorOpen, DashModel.Alert.LowFuel), DashModel.alerts(r))
        assertTrue(DashModel.Alert.DoorOpen.red)
    }

    @Test fun openDoorsText_listsOnlyOpenOnes() {
        val d = DashModel.Doors(frontLeft = true, frontRight = false, trunk = true)
        assertEquals("Motorista, Porta-malas", DashModel.openDoorsText(d))
    }

    @Test fun alerts_shift_onlyInSportMode_atTheShiftPoint() {
        val atShift = reading(speed = 60f).copy(rpm = 5000f)
        assertEquals(listOf(DashModel.Alert.Shift), DashModel.alerts(atShift, sport))
        assertTrue(DashModel.alerts(atShift.copy(rpm = 4900f), sport).isEmpty())
        // No dia a dia não tem faixa: só a cor da barra e do número.
        assertTrue(DashModel.alerts(reading(speed = 60f).copy(rpm = 3200f), daily).isEmpty())
    }

    @Test fun alerts_shift_comesBeforeEverythingElse() {
        val r = reading(speed = 60f, fuel = 5f).copy(rpm = 5200f, doors = DashModel.Doors(frontLeft = true))
        assertEquals(DashModel.Alert.Shift, DashModel.alerts(r, sport).first())
    }

    @Test fun alerts_doorOpenStopped_isNotAWarning() {
        // Parado, porta aberta é normal (e o app CAN da central já mostra popup).
        val r = reading(speed = 0f).copy(doors = DashModel.Doors(frontLeft = true))
        assertTrue(DashModel.alerts(r).isEmpty())
    }

}
