package com.rocha.jancarlauncher.obd

/**
 * Motor esquentando pela água (PID 05): ≥ 110 °C por 10 s liga o aviso; só desliga abaixo de 105 °C. Sem leitura,
 * desligado. (Depois de quente, o normal do EA111 fica entre ~85 e ~105 °C.)
 */
class OverheatRule {
    private var hotSince: Long? = null
    private var on = false

    fun update(coolantC: Float?, nowMs: Long): Boolean {
        if (coolantC == null) { hotSince = null; on = false; return false }
        hotSince = if (coolantC >= ON_C) hotSince ?: nowMs else null
        on = if (on) coolantC >= OFF_C else hotSince?.let { nowMs - it >= HOLD_MS } == true
        return on
    }

    companion object {
        const val ON_C = 110f
        const val OFF_C = 105f
        const val HOLD_MS = 10_000L
    }
}
