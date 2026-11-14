package com.rocha.jancarlauncher.ui.car3d

import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoxClusterTest {
    @Test fun sweep_isSymmetric_likeTheRealCluster() {
        assertEquals(-125f, FoxCluster.angle(0f, 0f, 8f), 0.001f)
        assertEquals(0f, FoxCluster.angle(4f, 0f, 8f), 0.001f) // 4 no topo
        assertEquals(0f, FoxCluster.angle(100f, 0f, 200f), 0.001f) // 100 no topo
        assertEquals(125f, FoxCluster.angle(260f, 0f, 200f), 0.001f) // passa do fim: para no fim
    }

    @Test fun fuel_isFractionOfThe50LiterTank() {
        assertEquals(0.5f, FoxCluster.fuelFraction(25f)!!, 0.001f)
        assertEquals(1f, FoxCluster.fuelFraction(60f)!!, 0.001f)
        assertNull(FoxCluster.fuelFraction(null))
    }

    @Test fun kmpl_noObd_noNeedle_stopped_zero() {
        assertNull(FoxCluster.kmplFraction(ObdClient.Live()))
        val stopped = ObdClient.Live(ObdClient.Status.Connected, litersPerHour = 0.7f, kmPerLiter = null)
        assertEquals(0f, FoxCluster.kmplFraction(stopped)!!, 0f)
        val moving = ObdClient.Live(ObdClient.Status.Connected, litersPerHour = 4f, kmPerLiter = 15f)
        assertEquals(0.5f, FoxCluster.kmplFraction(moving)!!, 0.001f)
    }

    @Test fun lcd_usesDotAndDashesLikeTheCar() {
        assertEquals("33.0 °C", FoxCluster.lcdTemp(33f))
        assertEquals("--- °C", FoxCluster.lcdTemp(null))
        assertEquals("68201", FoxCluster.lcdKm(68201))
        assertEquals("-----", FoxCluster.lcdKm(null))
        assertEquals("133.2", FoxCluster.lcdTrip(Trip(10.0, 133.2)))
        assertEquals("13.3", FoxCluster.lcdConsumption(Trip(10.0, 133.2)))
        assertEquals("---", FoxCluster.lcdConsumption(Trip()))
    }
}
