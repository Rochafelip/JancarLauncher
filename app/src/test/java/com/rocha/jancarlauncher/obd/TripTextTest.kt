package com.rocha.jancarlauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TripTextTest {
    private fun at(day: Int, h: Int, m: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, day, h, m, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val rec = TripRecord(at(17, 8, 12), at(17, 8, 40), movingS = 18 * 60.0, km = 12.4, liters = 0.94,
        ethanolLiters = 0.0, maxKmh = 98f)

    @Test fun main_withPrice() {
        assertEquals("12,4 km · 18 min · 13,2 km/L · R$ 5,82", TripText.main(rec, 619, 429))
    }

    @Test fun main_withoutPrice() {
        assertEquals("12,4 km · 18 min · 13,2 km/L", TripText.main(rec, null, null))
    }

    @Test fun main_tooLittleFuel_isDash_andLongTripInHours() {
        val r = rec.copy(liters = 0.01, movingS = 65 * 60.0)
        assertEquals("12,4 km · 1 h 05 min · — km/L", TripText.main(r, null, null))
    }

    @Test fun speeds() {
        assertEquals("média 41 km/h · máx 98 km/h", TripText.speeds(rec))
    }

    @Test fun whenText_todayYesterdayOrDate() {
        val now = at(17, 20, 0)
        assertEquals("hoje 08:12", TripText.whenText(rec, now))
        assertEquals("ontem 18:40", TripText.whenText(rec.copy(startWallMs = at(16, 18, 40)), now))
        assertEquals("14/11 07:55", TripText.whenText(rec.copy(startWallMs = at(14, 7, 55)), now))
    }
}
