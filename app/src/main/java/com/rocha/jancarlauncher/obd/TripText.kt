package com.rocha.jancarlauncher.obd

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Textos da aba Viagens (puros, testáveis). */
object TripText {
    private val ptBr = Locale("pt", "BR")

    /** "12,4 km · 18 min · 13,2 km/L · R$ 5,82" (o R$ só com os dois preços definidos). */
    fun main(r: TripRecord, gasCents: Int?, ethCents: Int?): String {
        val km = String.format(ptBr, "%.1f km", r.km)
        val kml = r.kmPerLiter?.let { String.format(ptBr, "%.1f km/L", it.coerceAtMost(99.9)) } ?: "— km/L"
        val cost = TripCost.reais(r.liters, r.ethanolLiters, gasCents, ethCents)?.let { TripCost.text(it) }
        return listOfNotNull(km, duration(r.movingS), kml, cost).joinToString(" · ")
    }

    /** "média 41 km/h · máx 98 km/h". */
    fun speeds(r: TripRecord): String =
        "média ${r.avgKmh?.roundToInt()?.toString() ?: "—"} km/h · máx ${r.maxKmh.roundToInt()} km/h"

    /** "hoje 08:12", "ontem 18:40" ou "14/11 07:55" (início da viagem). */
    fun whenText(r: TripRecord, nowWallMs: Long): String {
        val time = SimpleDateFormat("HH:mm", ptBr).format(Date(r.startWallMs))
        return when (dayDiff(r.startWallMs, nowWallMs)) {
            0 -> "hoje $time"
            1 -> "ontem $time"
            else -> SimpleDateFormat("dd/MM HH:mm", ptBr).format(Date(r.startWallMs))
        }
    }

    private fun duration(s: Double): String {
        val min = (s / 60.0).roundToInt()
        return if (min < 60) "$min min" else "${min / 60} h %02d min".format(min % 60)
    }

    private fun dayDiff(a: Long, b: Long): Int {
        fun day(ms: Long) = Calendar.getInstance().apply {
            timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((day(b) - day(a)) / 86_400_000.0).roundToInt()
    }
}
