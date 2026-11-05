package com.rocha.jancarlauncher.settings

import java.time.LocalTime

/** Janela noturna fixa (20:00-05:30, cruza a meia-noite). Sem sensor de luz: só o relógio. */
object NightModePolicy {
    val NIGHT_START: LocalTime = LocalTime.of(20, 0)
    val NIGHT_END: LocalTime = LocalTime.of(5, 30)
    const val NIGHT_BRIGHTNESS = 35

    fun isNight(now: LocalTime): Boolean = now >= NIGHT_START || now < NIGHT_END
}
