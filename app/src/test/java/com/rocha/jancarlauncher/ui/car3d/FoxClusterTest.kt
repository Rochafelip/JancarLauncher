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

    @Test fun coolant_scale50to130_noObdNoNeedle() {
        val on = { c: Float? -> ObdClient.Live(ObdClient.Status.Connected, coolantC = c) }
        assertEquals(0.5f, FoxCluster.coolantFraction(on(90f))!!, 0.001f) // 90 no topo, como no painel
        assertEquals(0f, FoxCluster.coolantFraction(on(25f))!!, 0f) // motor frio: encostado no começo
        assertEquals(1f, FoxCluster.coolantFraction(on(140f))!!, 0f)
        assertNull(FoxCluster.coolantFraction(on(null))) // o carro não informa
        assertNull(FoxCluster.coolantFraction(ObdClient.Live(coolantC = 90f))) // OBD desconectado
    }

    @Test fun shiftLeds_followTheMode() {
        val daily = com.rocha.jancarlauncher.ui.dash.DashModel.DriveMode.Daily
        val sport = com.rocha.jancarlauncher.ui.dash.DashModel.DriveMode.Sport
        assertEquals(0, FoxCluster.shiftLeds(1400f, daily)) // antes da faixa: aro normal
        assertEquals(4, FoxCluster.shiftLeds(2250f, daily)) // meio da faixa 1500→3000
        assertEquals(8, FoxCluster.shiftLeds(3000f, daily)) // no ponto: todos
        assertEquals(0, FoxCluster.shiftLeds(3000f, sport)) // no BRAVO a faixa começa em 3500
        assertEquals(8, FoxCluster.shiftLeds(5200f, sport))
        assertEquals(0, FoxCluster.shiftLeds(null, daily))
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 2, 2), (0 until 8).map { FoxCluster.ledZone(it) })
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
