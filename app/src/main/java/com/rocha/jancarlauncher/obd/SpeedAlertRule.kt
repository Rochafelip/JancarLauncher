package com.rocha.jancarlauncher.obd

enum class Beep { Double, Single }

/**
 * Aviso sonoro de velocidade: acima de limite + 2 km/h por 2 s → 2 bipes; continuando acima, 1 bipe a cada 20 s.
 * Só volta a dar os 2 bipes depois de cair abaixo de limite − 5 (oscilar em volta do limite não fica apitando).
 * Limite `null` = desligado.
 */
class SpeedAlertRule {
    private var overSince: Long? = null
    private var armed = true
    private var lastBeep: Long? = null

    fun update(speedKmh: Float?, limit: Int?, nowMs: Long): Beep? {
        if (limit == null) { overSince = null; armed = true; lastBeep = null; return null }
        if (speedKmh == null) { overSince = null; return null }
        if (!over(speedKmh, limit)) {
            overSince = null
            if (speedKmh < limit - REARM_BELOW) { armed = true; lastBeep = null }
            return null
        }
        val since = overSince ?: nowMs.also { overSince = it }
        if (armed) {
            if (nowMs - since < HOLD_MS) return null
            armed = false
            lastBeep = nowMs
            return Beep.Double
        }
        val last = lastBeep
        if (last != null && nowMs - last >= REPEAT_MS) {
            lastBeep = nowMs
            return Beep.Single
        }
        return null
    }

    companion object {
        const val MARGIN = 2
        const val REARM_BELOW = 5
        const val HOLD_MS = 2_000L
        const val REPEAT_MS = 20_000L

        /** Acima do limite (para o número da velocidade ficar vermelho). */
        fun over(speedKmh: Float?, limit: Int?): Boolean =
            speedKmh != null && limit != null && speedKmh >= limit + MARGIN
    }
}

/** Botões −/+ do limite: 40..150 km/h de 10 em 10; abaixo de 40 = desligado (`null`). */
fun stepLimit(limit: Int?, delta: Int): Int? = when {
    limit == null -> if (delta > 0) 40 else null
    limit + delta < 40 -> null
    else -> (limit + delta).coerceAtMost(150)
}
