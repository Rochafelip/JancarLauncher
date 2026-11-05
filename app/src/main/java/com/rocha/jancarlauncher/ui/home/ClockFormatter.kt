package com.rocha.jancarlauncher.ui.home

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockInfo(val time: String, val date: String)

object ClockFormatter {
    private val ptBr = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", ptBr)
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBr)

    fun format(now: LocalDateTime): ClockInfo = ClockInfo(
        time = now.format(timeFormat),
        date = now.format(dateFormat).replaceFirstChar { it.titlecase(ptBr) },
    )
}
