package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.ui.geometry.Size
import com.rocha.jancarlauncher.ui.dash.DashModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPhotoTest {
    @Test fun openDoors_onlyWhatTheCanSaysIsOpen() {
        val d = DashModel.Doors(frontLeft = true, frontRight = false, rearRight = true, trunk = null)
        assertEquals(setOf(CarPhoto.Door.FrontLeft, CarPhoto.Door.RearRight), CarPhoto.openDoors(d))
        assertTrue(CarPhoto.openDoors(DashModel.Doors()).isEmpty())
    }

    @Test fun fit_keepsAspectAndCenters() {
        // Foto 1000×500 numa caixa 600×600: cabe 600×300, sobra 150 em cima e embaixo.
        val (origin, size) = CarPhoto.fit(1000, 500, Size(600f, 600f))
        assertEquals(600f, size.width, 0.01f)
        assertEquals(300f, size.height, 0.01f)
        assertEquals(0f, origin.x, 0.01f)
        assertEquals(150f, origin.y, 0.01f)
    }

    @Test fun spec_coordinatesAreInsideThePhoto() {
        val s = CarPhoto.SPEC
        for (g in s.head + s.turnLeft + s.turnRight) {
            assertTrue(g.x in 0f..1f && g.y in 0f..1f && g.r > 0f && g.r < 0.2f)
        }
        for ((_, outline) in s.doors) {
            assertTrue(outline.size >= 3)
            assertTrue(outline.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test fun photoShowsTheDriverSide_passengerDoorsStayInTheText() {
        assertTrue(CarPhoto.Door.FrontLeft in CarPhoto.SPEC.doors)
        assertTrue(CarPhoto.Door.RearLeft in CarPhoto.SPEC.doors)
        assertTrue(CarPhoto.Door.FrontRight !in CarPhoto.SPEC.doors)
        assertTrue(CarPhoto.Door.RearRight !in CarPhoto.SPEC.doors)
        assertTrue(CarPhoto.Door.Trunk !in CarPhoto.SPEC.doors)
    }
}
