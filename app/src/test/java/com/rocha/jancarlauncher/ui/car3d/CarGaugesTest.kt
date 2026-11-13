package com.rocha.jancarlauncher.ui.car3d

import com.rocha.jancarlauncher.ui.dash.DashModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarGaugesTest {
    private val sport = DashModel.DriveMode.Sport // troca em 5000
    private val daily = DashModel.DriveMode.Daily // troca em 3000

    @Test fun noRpm_nothingLit() {
        assertTrue(CarGauges.stripSegments(null, sport).none { it.lit })
    }

    @Test fun litSegments_followRpm() {
        val segs = CarGauges.stripSegments(4000f, sport)
        val lit = segs.count { it.lit }
        // 48 segmentos para 8000 rpm: 4000 rpm acende metade.
        assertEquals(24, lit)
        assertTrue(segs.take(lit).all { it.lit })
    }

    @Test fun zones_followTheShiftPointOfTheMode() {
        val segs = CarGauges.stripSegments(8000f, sport)
        val segRpm = { i: Int -> CarGauges.RPM_SCALE_MAX * (i + 0.5f) / CarGauges.STRIP_SEGMENTS }
        segs.forEachIndexed { i, s ->
            val expected = when {
                segRpm(i) >= 5000 -> CarGauges.Zone.Shift
                segRpm(i) >= 4500 -> CarGauges.Zone.Near
                else -> CarGauges.Zone.Normal
            }
            assertEquals("segmento $i", expected, s.zone)
        }
        // No dia a dia o vermelho começa bem antes.
        assertTrue(CarGauges.stripSegments(8000f, daily).count { it.zone == CarGauges.Zone.Shift } >
            segs.count { it.zone == CarGauges.Zone.Shift })
    }

    @Test fun segmentsGrowFromLeftToRight() {
        val h = CarGauges.stripSegments(0f, sport).map { it.heightFraction }
        assertTrue(h.zipWithNext().all { (a, b) -> b > a })
        assertEquals(1f, h.last(), 1e-6f)
    }

    @Test fun fuel_isShareOfTheTank_andUnknownStaysUnknown() {
        assertEquals(5, CarGauges.fuelSegments(CarGauges.FUEL_TANK_L / 2))
        assertEquals(10, CarGauges.fuelSegments(CarGauges.FUEL_TANK_L + 5f))
        assertEquals(0, CarGauges.fuelSegments(0f))
        assertNull(CarGauges.fuelSegments(null))
        assertFalse(CarGauges.fuelSegments(49f) == null)
    }
}
