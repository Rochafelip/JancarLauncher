package com.rocha.jancarlauncher.ui.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashModelTest {
    // --- barra de RPM ---

    @Test fun litSegments_unknownRpm_isZero() {
        assertEquals(0, DashModel.litSegments(null))
    }

    @Test fun litSegments_engineOff_isZero() {
        assertEquals(0, DashModel.litSegments(0f))
    }

    @Test fun litSegments_halfScale_lightsHalfTheBar() {
        assertEquals(DashModel.SEGMENTS / 2, DashModel.litSegments(DashModel.RPM_MAX / 2f))
    }

    @Test fun litSegments_atOrAboveMax_isFullBar_neverOverflows() {
        assertEquals(DashModel.SEGMENTS, DashModel.litSegments(DashModel.RPM_MAX.toFloat()))
        assertEquals(DashModel.SEGMENTS, DashModel.litSegments(DashModel.RPM_MAX * 3f))
    }

    @Test fun litSegments_idle_lightsAtLeastOneSegment() {
        // Marcha lenta (~850 rpm) precisa aparecer na barra, não ficar apagada.
        assertTrue(DashModel.litSegments(850f) >= 1)
    }

    @Test fun zone_lowSegments_areNormal() {
        assertEquals(DashModel.Zone.Normal, DashModel.zone(0))
    }

    @Test fun zone_segmentStartingAtAmberRpm_isWarn() {
        assertEquals(DashModel.Zone.Warn, DashModel.zone(DashModel.segmentFor(DashModel.RPM_WARN)))
    }

    @Test fun zone_segmentJustBeforeAmber_isNormal() {
        assertEquals(DashModel.Zone.Normal, DashModel.zone(DashModel.segmentFor(DashModel.RPM_WARN) - 1))
    }

    @Test fun zone_segmentStartingAtRedRpm_isRed_andLastSegmentIsRed() {
        assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.segmentFor(DashModel.RPM_RED)))
        assertEquals(DashModel.Zone.Red, DashModel.zone(DashModel.SEGMENTS - 1))
    }

    // --- aviso de troca ---

    @Test fun shiftLight_offBelowShiftPoint() {
        assertFalse(DashModel.shiftLight(DashModel.RPM_SHIFT - 1f))
        assertFalse(DashModel.shiftLight(null))
    }

    @Test fun shiftLight_onAtShiftPoint() {
        assertTrue(DashModel.shiftLight(DashModel.RPM_SHIFT.toFloat()))
    }

    // --- textos ---

    @Test fun speedText_unknown_isSingleDash() {
        assertEquals("—", DashModel.speedText(null))
    }

    @Test fun speedText_roundsToWholeKmh() {
        assertEquals("88", DashModel.speedText(87.6f))
        assertEquals("0", DashModel.speedText(0.2f))
    }

    @Test fun speedText_clampsToThreeDigits() {
        assertEquals("999", DashModel.speedText(1500f))
    }

    @Test fun rpmText_unknown_isSingleDash() {
        assertEquals("—", DashModel.rpmText(null))
    }

    @Test fun rpmText_roundsToTens_toAvoidFlicker() {
        assertEquals("3460", DashModel.rpmText(3456f))
        assertEquals("850", DashModel.rpmText(851f))
    }

    @Test fun odoText_unknown_isSingleDash() {
        assertEquals("—", DashModel.odoText(null))
    }

    @Test fun odoText_zeroOrNegative_isTreatedAsMissingData() {
        // Odômetro zerado/negativo num carro rodado = CAN sem dado, não "0 km".
        assertEquals("—", DashModel.odoText(0))
        assertEquals("—", DashModel.odoText(-1))
    }

    @Test fun odoText_usesThousandsSeparatorWithDot() {
        assertEquals("123.456", DashModel.odoText(123456))
        assertEquals("987", DashModel.odoText(987))
    }

    @Test fun valueText_oneDecimalWithComma_orSingleDash() {
        assertEquals("12,4", DashModel.valueText(12.38f))
        assertEquals("—", DashModel.valueText(null))
    }
}
