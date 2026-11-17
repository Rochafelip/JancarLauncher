package com.rocha.jancarlauncher.obd

enum class BatteryAlert { NotCharging, HighVoltage, Weak }

/**
 * Saúde da bateria pela tensão do `ATRV` (só aviso, nunca número no Painel). Faixas típicas de carro 12 V:
 * motor ligado 13,8–14,6 V (alternador carregando); motor desligado ~12,6 V. Avisa só com a condição mantida por
 * 20 s, para picos e a queda da partida não dispararem nada; sai com 0,3 V de folga.
 */
class BatteryRule {
    private var engineOnSince: Long? = null
    private var lowSince: Long? = null
    private var highSince: Long? = null
    private var weakSince: Long? = null
    private var current: BatteryAlert? = null

    fun update(volts: Float?, rpm: Float?, nowMs: Long): BatteryAlert? {
        if (volts == null) {
            lowSince = null; highSince = null; weakSince = null; current = null
            return null
        }
        val running = rpm != null && rpm > RUNNING_RPM
        engineOnSince = if (running) engineOnSince ?: nowMs else null
        val warm = engineOnSince?.let { nowMs - it > WARMUP_MS } == true

        lowSince = if (warm && volts < LOW_RUNNING_V) lowSince ?: nowMs else null
        highSince = if (running && volts > HIGH_V) highSince ?: nowMs else null
        weakSince = if (!running && volts < WEAK_OFF_V) weakSince ?: nowMs else null

        // Sai do aviso atual só com folga (ou quando a condição de motor não vale mais).
        current = when (current) {
            BatteryAlert.NotCharging -> BatteryAlert.NotCharging.takeIf { running && volts < LOW_RUNNING_V + HYST_V }
            BatteryAlert.HighVoltage -> BatteryAlert.HighVoltage.takeIf { running && volts > HIGH_V - HYST_V }
            BatteryAlert.Weak -> BatteryAlert.Weak.takeIf { !running && volts < WEAK_OFF_V + HYST_V }
            null -> null
        }
        if (current == null) {
            current = when {
                held(lowSince, nowMs) -> BatteryAlert.NotCharging
                held(highSince, nowMs) -> BatteryAlert.HighVoltage
                held(weakSince, nowMs) -> BatteryAlert.Weak
                else -> null
            }
        }
        return current
    }

    private fun held(since: Long?, now: Long) = since != null && now - since >= HOLD_MS

    companion object {
        const val RUNNING_RPM = 500f
        /** Motor ligado há mais que isto antes de cobrar carga (a partida e o começo puxam a tensão). */
        const val WARMUP_MS = 30_000L
        const val HOLD_MS = 20_000L
        const val LOW_RUNNING_V = 13.0f
        const val HIGH_V = 15.0f
        const val WEAK_OFF_V = 12.2f
        const val HYST_V = 0.3f
    }
}
