package com.rocha.jancarlauncher.ui.car3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClusterTest {
    @Test fun zero_pointsAtStartOfArc() {
        assertEquals(Cluster.START_DEG, Cluster.angle(0f, 8f), 0.001f)
    }

    @Test fun fullScale_pointsAtEndOfArc() {
        assertEquals(Cluster.START_DEG + Cluster.SWEEP_DEG, Cluster.angle(240f, 240f), 0.001f)
    }

    @Test fun beyondScale_stopsAtThePeg() {
        assertEquals(Cluster.angle(8f, 8f), Cluster.angle(9.5f, 8f), 0.001f)
        assertEquals(Cluster.angle(0f, 8f), Cluster.angle(-1f, 8f), 0.001f)
    }

    @Test fun halfScale_isHalfTheSweep() {
        assertEquals(Cluster.START_DEG + Cluster.SWEEP_DEG / 2, Cluster.angle(4f, 8f), 0.001f)
    }

    @Test fun fuel_isFractionOfTank_andUnknownStaysUnknown() {
        assertEquals(0.5f, Cluster.fuelFraction(Cluster.FUEL_TANK_L / 2)!!, 0.001f)
        assertEquals(1f, Cluster.fuelFraction(Cluster.FUEL_TANK_L + 5f)!!, 0.001f)
        assertNull(Cluster.fuelFraction(null))
    }
}
