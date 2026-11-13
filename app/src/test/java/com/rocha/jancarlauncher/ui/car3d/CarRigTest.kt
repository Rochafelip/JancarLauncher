package com.rocha.jancarlauncher.ui.car3d

import com.rocha.jancarlauncher.ui.dash.DashModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarRigTest {

    @Test
    fun `porta aberta no CAN abre a peca certa`() {
        val d = DashModel.Doors(frontLeft = true, trunk = true, frontRight = false)
        assertTrue(CarRig.isOpen(CarRig.Part.DoorFrontLeft, d))
        assertTrue(CarRig.isOpen(CarRig.Part.Tailgate, d))
        assertFalse(CarRig.isOpen(CarRig.Part.DoorFrontRight, d))
        // Sem dado (null) = desenhada fechada, nunca inventa porta aberta.
        assertFalse(CarRig.isOpen(CarRig.Part.Hood, d))
    }

    @Test
    fun `portas da esquerda e da direita giram para lados opostos`() {
        assertTrue(CarRig.Part.DoorFrontLeft.openDegrees < 0f)
        assertTrue(CarRig.Part.DoorRearLeft.openDegrees < 0f)
        assertTrue(CarRig.Part.DoorFrontRight.openDegrees > 0f)
        assertTrue(CarRig.Part.DoorRearRight.openDegrees > 0f)
    }

    @Test
    fun `animacao abre aos poucos e para no fim`() {
        val a = CarAnimator()
        a.setDoors(DashModel.Doors(frontLeft = true))
        assertTrue(a.step(CarRig.SWING_MS / 2))
        assertEquals(0.5f, a.progress(CarRig.Part.DoorFrontLeft), 1e-4f)
        assertFalse(a.step(CarRig.SWING_MS))
        assertEquals(1f, a.progress(CarRig.Part.DoorFrontLeft), 0f)
        assertEquals(CarRig.Part.DoorFrontLeft.openDegrees, a.angle(CarRig.Part.DoorFrontLeft), 1e-4f)
        // Nada mudou: não pede mais quadros.
        assertFalse(a.step(16f))
    }

    @Test
    fun `fechar no meio do movimento volta do ponto onde estava`() {
        val a = CarAnimator()
        a.setDoors(DashModel.Doors(trunk = true))
        a.step(CarRig.SWING_MS * 0.3f)
        a.setDoors(DashModel.Doors(trunk = false))
        a.step(CarRig.SWING_MS * 0.1f)
        assertEquals(0.2f, a.progress(CarRig.Part.Tailgate), 1e-4f)
    }

    @Test
    fun `avisa so a peca que acabou de abrir`() {
        val a = CarAnimator()
        assertEquals(CarRig.Part.DoorRearRight, a.setDoors(DashModel.Doors(rearRight = true)))
        // Continua aberta: não é "acabou de abrir" de novo.
        assertNull(a.setDoors(DashModel.Doors(rearRight = true)))
        assertNull(a.setDoors(DashModel.Doors()))
    }

    @Test
    fun `camera gira pelo caminho mais curto`() {
        assertEquals(-70f, CarRenderer.nearestAngle(10f, -70f), 0f)
        assertEquals(290f, CarRenderer.nearestAngle(350f, -70f), 0f)
        assertEquals(-205f, CarRenderer.nearestAngle(-100f, 155f), 0f)
    }

    @Test
    fun `suavizacao comeca e termina parada`() {
        assertEquals(0f, CarRig.ease(0f), 0f)
        assertEquals(1f, CarRig.ease(1f), 0f)
        assertEquals(0.5f, CarRig.ease(0.5f), 1e-6f)
        assertEquals(1f, CarRig.ease(2f), 0f)
    }
}
