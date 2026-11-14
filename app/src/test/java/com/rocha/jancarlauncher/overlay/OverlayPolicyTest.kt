package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.ui.dash.DashModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPolicyTest {
    private val aa = "com.google.android.projection.sink"

    @Test fun showsOnlyOverProjectionApps_andOnlyWithPermission() {
        assertTrue(OverlayPolicy.shouldShow(aa, canDraw = true))
        assertTrue(OverlayPolicy.shouldShow("com.autochips.carplayapp", canDraw = true))
        assertFalse(OverlayPolicy.shouldShow(aa, canDraw = false))
        assertFalse(OverlayPolicy.shouldShow("com.jancar.radio", canDraw = true))
        assertFalse(OverlayPolicy.shouldShow(null, canDraw = true))
        assertTrue(OverlayPolicy.shouldShow("com.android.settings", canDraw = true, extraTarget = "com.android.settings"))
    }

    @Test fun lines_hideWhatHasNoData() {
        val r = DashModel.Reading(speed = null, rpm = null, fuelLiters = null, outsideTempC = null, odoKm = null)
        val l = OverlayPolicy.lines(r, ObdClient.Live(), DashModel.DriveMode.Daily)
        assertTrue(l.isEmpty)
        assertNull(l.consumption)
    }

    @Test fun lines_withData_andShiftStageFromMode() {
        val r = DashModel.Reading(speed = 62f, rpm = 3100f, fuelLiters = null, outsideTempC = null, odoKm = null)
        val live = ObdClient.Live(ObdClient.Status.Connected, speedKmh = 62f, litersPerHour = 4.4f, kmPerLiter = 14.1f)
        val l = OverlayPolicy.lines(r, live, DashModel.DriveMode.Daily)
        assertEquals("62", l.speed)
        assertEquals("3100", l.rpm)
        assertEquals(DashModel.ShiftStage.Shift, l.stage)
        assertEquals("14,1 km/L", l.consumption)
        assertEquals(DashModel.ShiftStage.None, OverlayPolicy.lines(r, live, DashModel.DriveMode.Sport).stage)
    }
}
