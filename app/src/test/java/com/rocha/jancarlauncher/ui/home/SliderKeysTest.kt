package com.rocha.jancarlauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class SliderKeysTest {
    private val changes = mutableListOf<Int>()
    private var finished = 0

    private fun press(value: Int, direction: Int, min: Int = 10, max: Int = 100) =
        SliderKeys.step(value, direction, min, max, onChange = { changes += it }, onFinished = { finished++ })

    @Test fun right_raisesByStep_andPersists() {
        press(50, +1)
        assertEquals(listOf(55), changes)
        assertEquals(1, finished) // sem isto o valor vindo do D-pad nunca era salvo
    }

    @Test fun left_lowersByStep_andPersists() {
        press(50, -1)
        assertEquals(listOf(45), changes)
        assertEquals(1, finished)
    }

    @Test fun clampsAtMax() {
        press(98, +1)
        assertEquals(listOf(100), changes)
    }

    @Test fun clampsAtMin() {
        press(12, -1)
        assertEquals(listOf(10), changes)
    }

    @Test fun stepIsFive() = assertEquals(5, SliderKeys.STEP)
}
