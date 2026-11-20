package com.rocha.jancarlauncher.wheel

import org.junit.Assert.assertEquals
import org.junit.Test

class WheelKeysTest {
    @Test fun knownKeysHaveNames() {
        assertEquals("Volume +", WheelKeys.name(1))
        assertEquals("Próxima", WheelKeys.name(32))
        assertEquals("Anterior", WheelKeys.name(33))
        assertEquals("Play/Pause", WheelKeys.name(11))
    }

    @Test fun unknownKeyKeepsItsNumber() = assertEquals("tecla 777", WheelKeys.name(777))

    @Test fun typesAreLabelled() {
        assertEquals("apertou", WheelKeys.typeLabel(WheelKeys.TYPE_DOWN))
        assertEquals("soltou", WheelKeys.typeLabel(WheelKeys.TYPE_UP))
        assertEquals("tipo 9", WheelKeys.typeLabel(9))
    }
}
