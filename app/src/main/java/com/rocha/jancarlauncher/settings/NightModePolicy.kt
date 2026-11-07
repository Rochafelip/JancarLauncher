package com.rocha.jancarlauncher.settings

import java.time.LocalTime

/**
 * Quando é "noite" para o modo noturno. Na central o farol manda (fio de iluminação do chicote, lido pelo
 * ICar; validado no Fox em 2026-11-07): aceso = noite, apagado = dia. Sem leitura do farol (ICar
 * indisponível, ex.: emulador) vale a janela fixa 20:00-05:30, que cruza a meia-noite.
 */
object NightModePolicy {
    val NIGHT_START: LocalTime = LocalTime.of(20, 0)
    val NIGHT_END: LocalTime = LocalTime.of(5, 30)
    const val NIGHT_BRIGHTNESS = 35

    fun isNight(now: LocalTime): Boolean = now >= NIGHT_START || now < NIGHT_END

    fun isNight(headlightOn: Boolean?, now: LocalTime): Boolean = headlightOn ?: isNight(now)
}
