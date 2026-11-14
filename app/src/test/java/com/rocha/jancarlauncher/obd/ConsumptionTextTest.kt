package com.rocha.jancarlauncher.obd

import com.rocha.jancarlauncher.ui.dash.ConsumptionText
import com.rocha.jancarlauncher.ui.dash.DashModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsumptionTextTest {
    private val connected = ObdClient.Live(ObdClient.Status.Connected, "OBDII")

    @Test fun notConnected_showsDashNeverANumber() {
        assertEquals(DashModel.DASH, ConsumptionText.instant(ObdClient.Live(ObdClient.Status.Connecting, kmPerLiter = 14f)).first)
        assertEquals("Adaptador OBD não pareado", ConsumptionText.status(ObdClient.Live(ObdClient.Status.NoAdapter)))
    }

    @Test fun moving_showsKmPerLiter_stopped_showsLitersPerHour() {
        assertEquals("km/L", ConsumptionText.instant(connected.copy(kmPerLiter = 14.1f, litersPerHour = 4.4f)).second)
        assertEquals("L/h", ConsumptionText.instant(connected.copy(kmPerLiter = null, litersPerHour = 0.7f)).second)
        assertNull(ConsumptionText.status(connected))
    }

    @Test fun trip_withoutEnoughData_isDash() {
        assertEquals(DashModel.DASH, ConsumptionText.trip(Trip()))
    }
}
