package com.rocha.jancarlauncher.ui.car3d

import androidx.compose.ui.geometry.Offset
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
        for ((_, at) in s.doors) assertTrue(at.x in 0f..1f && at.y in 0f..1f)
    }

    @Test fun everyDoorHasATagPoint_evenTheHiddenOnes() {
        // O texto "Aberto: ..." saiu da tela: o balão é o único aviso, então toda porta precisa de um ponto.
        assertEquals(CarPhoto.Door.entries.toSet(), CarPhoto.SPEC.doors.keys)
    }

    @Test fun placeTags_centersAboveThePointAndStaysInsideTheBox() {
        val tags = CarPhoto.placeTags(
            listOf(CarPhoto.Door.Trunk to Offset(390f, 100f)),
            mapOf(CarPhoto.Door.Trunk to Size(80f, 20f)), boxWidth = 400f, lift = 10f, gap = 4f,
        )
        val t = tags.single()
        assertEquals(320f, t.left, 0.01f) // encostado na direita, não sai da caixa
        assertEquals(70f, t.top, 0.01f)   // 100 - 10 de haste - 20 de altura
    }

    @Test fun placeTags_neverOverlaps_theUpperOneMovesUp() {
        val s = Size(90f, 22f)
        val doors = listOf(
            CarPhoto.Door.FrontLeft to Offset(270f, 105f),
            CarPhoto.Door.RearLeft to Offset(300f, 100f),
            CarPhoto.Door.Trunk to Offset(330f, 95f),
        )
        val tags = CarPhoto.placeTags(doors, doors.associate { it.first to s }, boxWidth = 372f, lift = 9f, gap = 4f)
        assertEquals(3, tags.size)
        for (a in tags) for (b in tags) if (a !== b) {
            val apart = a.right + 4f <= b.left || b.right + 4f <= a.left || a.bottom + 4f <= b.top || b.bottom + 4f <= a.top
            assertTrue("${a.door} encosta em ${b.door}", apart)
        }
        // O de ponto mais baixo fica no lugar; os outros sobem.
        assertEquals(105f - 9f - 22f, tags.first { it.door == CarPhoto.Door.FrontLeft }.top, 0.01f)
    }
}
