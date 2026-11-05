package com.rocha.jancarlauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessMapperTest {
    @Test fun systemValue_atMax_is255() = assertEquals(255, BrightnessMapper.toSystem(100))

    @Test fun systemValue_atMin10_is26() = assertEquals(26, BrightnessMapper.toSystem(10))

    @Test fun systemValue_belowMin_isClampedTo10Percent() = assertEquals(26, BrightnessMapper.toSystem(0))

    @Test fun systemValue_aboveMax_isClampedTo255() = assertEquals(255, BrightnessMapper.toSystem(150))

    @Test fun systemValue_middle() = assertEquals(128, BrightnessMapper.toSystem(50))

    @Test fun windowValue_isFractionOfOne() {
        assertEquals(0.7f, BrightnessMapper.toWindow(70), 0.0001f)
        assertEquals(0.1f, BrightnessMapper.toWindow(-5), 0.0001f)
        assertEquals(1f, BrightnessMapper.toWindow(500), 0.0001f)
    }

    @Test fun fromSystem_convertsBackToPercent() {
        assertEquals(100, BrightnessMapper.fromSystem(255))
        assertEquals(50, BrightnessMapper.fromSystem(128))
        assertEquals(10, BrightnessMapper.fromSystem(26))
    }

    @Test fun fromSystem_clampsOutOfRange() {
        assertEquals(10, BrightnessMapper.fromSystem(0))
        assertEquals(100, BrightnessMapper.fromSystem(999))
    }

    @Test fun clamp_keepsRange() {
        assertEquals(10, BrightnessMapper.clamp(3))
        assertEquals(100, BrightnessMapper.clamp(101))
        assertEquals(42, BrightnessMapper.clamp(42))
    }
}
