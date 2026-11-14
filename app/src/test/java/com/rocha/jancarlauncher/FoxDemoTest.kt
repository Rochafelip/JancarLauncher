package com.rocha.jancarlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoxDemoTest {
    @Test fun gearChange_dropsRpm_likeARealShift() {
        val before = FoxDemo.rpm(30.5f) // fim da 2ª (~3080)
        val after = FoxDemo.rpm(31f) // começo da 3ª
        assertTrue("troca derruba o giro: $before -> $after", after < before - 800)
        assertTrue(before <= 3100f)
    }

    @Test fun demo_showsUpAndDownHints() {
        val mode = com.rocha.jancarlauncher.ui.dash.DashModel.DriveMode.Daily
        val hints = (0 until 320).map { FoxDemo.at(it / 10.0).reading }.map {
            val g = com.rocha.jancarlauncher.ui.dash.GearModel.gearFor(it.rpm, it.speed)
            com.rocha.jancarlauncher.ui.dash.GearModel.suggest(g, it.rpm, it.speed, mode).hint
        }
        assertTrue(com.rocha.jancarlauncher.ui.dash.GearModel.Hint.Up in hints)
        assertTrue(com.rocha.jancarlauncher.ui.dash.GearModel.Hint.Down in hints)
    }

    @Test fun script_coversEverythingTheScreenShows() {
        val frames = (0 until 320).map { FoxDemo.at(it / 10.0) }
        assertTrue(frames.any { it.reading.doors.frontLeft == true })
        assertTrue(frames.any { it.reading.doors.trunk == true })
        assertTrue(frames.any { it.reading.headlightOn == true })
        assertTrue(frames.any { it.reading.turnLeft == true && it.reading.turnRight != true })
        assertTrue(frames.any { it.reading.turnRight == true && it.reading.turnLeft != true })
        assertTrue(frames.any { it.reading.turnLeft == true && it.reading.turnRight == true })
        assertEquals(75f, frames.maxOf { it.reading.speed ?: 0f }, 0.5f)
        assertTrue(frames.any { it.obd.kmPerLiter != null })
    }
}
