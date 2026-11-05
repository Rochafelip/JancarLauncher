package com.rocha.jancarlauncher.ui.home

import kotlin.math.roundToInt

/** Brilho em porcentagem (10–100) ↔ valores do Android. Mínimo de 10% evita tela apagada sem querer. */
object BrightnessMapper {
    const val MIN = 10
    const val MAX = 100

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)

    /** Valor de `Settings.System.SCREEN_BRIGHTNESS` (0–255). */
    fun toSystem(percent: Int): Int = (clamp(percent) * 255 / 100f).roundToInt()

    /** Valor de `Settings.System.SCREEN_BRIGHTNESS` (0–255) → porcentagem (10–100). */
    fun fromSystem(value: Int): Int = clamp((value * 100 / 255f).roundToInt())

    /** Valor de `WindowManager.LayoutParams.screenBrightness` (0–1). */
    fun toWindow(percent: Int): Float = clamp(percent) / 100f
}
