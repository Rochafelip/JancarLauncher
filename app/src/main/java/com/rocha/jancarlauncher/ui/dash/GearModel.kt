package com.rocha.jancarlauncher.ui.dash

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln

/**
 * Marcha engatada pela relação RPM ÷ km/h e sugestão de troca (pedido do dono, 2026-11-14: "bater o RPM e o km/h e
 * sugerir uma marcha"; câmbio original). Puro, testável.
 *
 * Relações do Fox 1.0 8V (EA111) com câmbio de 5 marchas, das fichas técnicas (Vrum Fox 2011, Autoo Fox 2010 Trend):
 * diferencial 4,929; 2ª 2,30; 3ª 1,433; 4ª 0,975; 5ª 0,776 (todas concordam). A 1ª diverge entre fichas (4,167 ou
 * 4,267): usa a média, e a tolerância cobre as duas. Pneu 195/55 R15 (ficha informada pelo dono).
 */
object GearModel {
    private const val FINAL_DRIVE = 4.929
    private val GEAR_RATIOS = doubleArrayOf(4.217, 2.30, 1.433, 0.975, 0.776)

    /** Circunferência do pneu 195/55 R15: aro 15" + 2 × (195 × 0,55) mm. */
    private val TIRE_CIRCUMFERENCE_M = PI * (15 * 25.4 + 2 * 195 * 0.55) / 1000.0

    /** Giro por km/h em cada marcha (1ª..5ª). */
    val RPM_PER_KMH: DoubleArray = DoubleArray(GEAR_RATIOS.size) { i ->
        GEAR_RATIOS[i] * FINAL_DRIVE * (1000.0 / 60.0) / TIRE_CIRCUMFERENCE_M
    }

    /** Fora de ±11% de qualquer marcha = embreagem pisada, ponto morto ou trocando: sem marcha. 4ª↔5ª distam 26%. */
    private val TOLERANCE = ln(1.11)
    const val MIN_SPEED_KMH = 6f
    const val MIN_RPM = 900f

    /** Abaixo disto o motor "pede" a marcha de baixo (por modo: no BRAVO o giro fica perto do torque de propósito). */
    fun downshiftRpm(mode: DashModel.DriveMode): Float = if (mode == DashModel.DriveMode.Sport) 2300f else 1400f

    /** Marcha (1..5) que bate com a leitura agora; `null` = nenhuma bate (ou sem dado / parado). */
    fun gearFor(rpm: Float?, speed: Float?): Int? {
        if (rpm == null || speed == null || speed < MIN_SPEED_KMH || rpm < MIN_RPM) return null
        val observed = ln(rpm / speed.toDouble())
        var best = -1
        var bestDist = Double.MAX_VALUE
        for (i in RPM_PER_KMH.indices) {
            val d = abs(observed - ln(RPM_PER_KMH[i]))
            if (d < bestDist) { bestDist = d; best = i }
        }
        return if (bestDist <= TOLERANCE) best + 1 else null
    }

    enum class Hint { None, Up, Down }

    data class Suggestion(val hint: Hint, val target: Int?)

    /**
     * Sugestão: subir quando o giro chega perto do ponto de troca do modo (mesmo âmbar do Painel); descer quando o
     * giro cai abaixo de [downshiftRpm] andando e a marcha de baixo não passaria do ponto de troca.
     */
    fun suggest(gear: Int?, rpm: Float?, speed: Float?, mode: DashModel.DriveMode): Suggestion {
        if (gear == null || rpm == null || speed == null) return Suggestion(Hint.None, null)
        val stage = DashModel.shiftStage(rpm, mode)
        if (gear < RPM_PER_KMH.size && stage != DashModel.ShiftStage.None) return Suggestion(Hint.Up, gear + 1)
        if (gear > 1 && rpm < downshiftRpm(mode)) {
            val lowerRpm = speed * RPM_PER_KMH[gear - 2]
            if (lowerRpm < mode.shiftRpm - DashModel.SHIFT_APPROACH_RPM) return Suggestion(Hint.Down, gear - 1)
        }
        return Suggestion(Hint.None, null)
    }

    fun gearText(gear: Int?): String = gear?.toString() ?: DashModel.DASH

    fun hintText(s: Suggestion): String? = when (s.hint) {
        Hint.Up -> "↑${s.target}"
        Hint.Down -> "↓${s.target}"
        Hint.None -> null
    }
}

/**
 * Segura a marcha só depois de [STABLE_MS] batendo igual: na troca o giro passa por valores no meio do caminho e a
 * marcha piscaria. E se a leitura passa [STABLE_MS] sem bater com a marcha mostrada (reduzindo marcha atrás de marcha,
 * embreagem), a marcha some em vez de ficar uma velha na tela. Uma instância por tela (cada uma com seu relógio).
 */
class GearEstimator {
    private var candidate: Int? = null
    private var since = 0L
    private var mismatchSince: Long? = null
    var gear: Int? = null
        private set

    fun update(rpm: Float?, speed: Float?, nowMs: Long): Int? {
        val g = GearModel.gearFor(rpm, speed)
        if (g != candidate) {
            candidate = g
            since = nowMs
        }
        if (g == gear) {
            mismatchSince = null
        } else if (mismatchSince == null) {
            mismatchSince = nowMs
        }
        when {
            nowMs - since >= STABLE_MS -> { gear = g; mismatchSince = null }
            mismatchSince?.let { nowMs - it >= STABLE_MS } == true -> gear = null
        }
        return gear
    }

    companion object {
        const val STABLE_MS = 600L
    }
}
