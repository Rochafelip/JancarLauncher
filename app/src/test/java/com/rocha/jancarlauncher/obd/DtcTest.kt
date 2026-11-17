package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtcTest {
    @Test fun kwp_threeCodesPerLine_noCountByte() {
        assertEquals(listOf("P0133", "P0201"), Dtc.parse("43013302010000", 3))
    }

    @Test fun canSingleFrame_hasCountByte() {
        assertEquals(listOf("P0133", "P0301"), Dtc.parse("430201330301", 3))
    }

    @Test fun canMultiFrame_joinsFramesInOrder() {
        assertEquals(
            listOf("P0133", "P0301", "P0420", "P0171"),
            Dtc.parse("00A\r0:430401330301\r1:04200171000000", 3),
        )
    }

    @Test fun noCodes_isEmpty_badAnswer_isNull() {
        assertEquals(emptyList<String>(), Dtc.parse("NO DATA", 3))
        assertEquals(emptyList<String>(), Dtc.parse("43", 3))
        assertNull(Dtc.parse("?", 3))
        assertNull(Dtc.parse("UNABLE TO CONNECT", 3))
        assertNull(Dtc.parse("", 3))
    }

    @Test fun letterComesFromTopTwoBits() {
        assertEquals("C0123", Dtc.code(0x41, 0x23))
        assertEquals("B0100", Dtc.code(0x81, 0x00))
        assertEquals("U0100", Dtc.code(0xC1, 0x00))
        assertEquals("P2096", Dtc.code(0x20, 0x96))
    }

    @Test fun pendingMode07() {
        assertEquals(listOf("P0100"), Dtc.parse("47010000000000", 7))
    }

    @Test fun twoEcus_noRepeats() {
        assertEquals(listOf("P0133", "P0301"), Dtc.parse("430101330000\r430103010000\r430101330000", 3))
    }

    @Test fun milStatus_fromPid01() {
        assertEquals(MilStatus(true, 3), Dtc.milStatus("410183076500"))
        assertEquals(MilStatus(false, 0), Dtc.milStatus("410100076500"))
        assertNull(Dtc.milStatus("NO DATA"))
    }
}
