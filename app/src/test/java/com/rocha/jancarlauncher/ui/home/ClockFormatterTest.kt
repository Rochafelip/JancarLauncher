package com.rocha.jancarlauncher.ui.home

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatterTest {
    @Test fun formatsTime24h_andPortugueseCapitalizedDate() {
        val info = ClockFormatter.format(LocalDateTime.of(2026, 11, 5, 18, 37))
        assertEquals("18:37", info.time)
        assertEquals("Quinta-feira, 5 de novembro", info.date)
    }

    @Test fun padsHourAndMinute() {
        assertEquals("07:05", ClockFormatter.format(LocalDateTime.of(2026, 1, 2, 7, 5)).time)
    }

    @Test fun midnight_is00h00() {
        assertEquals("00:00", ClockFormatter.format(LocalDateTime.of(2026, 3, 1, 0, 0)).time)
    }

    @Test fun otherWeekday_isCapitalized() {
        assertEquals("Domingo, 1 de março", ClockFormatter.format(LocalDateTime.of(2026, 3, 1, 12, 0)).date)
    }
}
