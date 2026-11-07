package com.rocha.jancarlauncher.ui.dash

import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Regras do Painel (velocidade/RPM em estilo de painel esportivo). Faixas pensadas para o VW Fox:
 * ajuste só aqui se o corte de giro real for diferente.
 */
object DashModel {
    const val RPM_MAX = 7000
    /** Início da faixa âmbar. */
    const val RPM_WARN = 4500
    /** Início da faixa vermelha. */
    const val RPM_RED = 5500
    /** A partir daqui a barra pisca (hora de trocar a marcha). */
    const val RPM_SHIFT = 5800
    /** Segmentos da barra (200 rpm cada). */
    const val SEGMENTS = 35

    /** Valor desconhecido: um traço só (em fonte grande, "--" vira dois blocos soltos). */
    const val DASH = "—"

    private const val STEP = RPM_MAX.toFloat() / SEGMENTS
    private val ptBr = Locale.forLanguageTag("pt-BR")

    enum class Zone { Normal, Warn, Red }

    /** Quantos segmentos acendem; qualquer giro > 0 acende pelo menos 1 (marcha lenta aparece). */
    fun litSegments(rpm: Float?): Int {
        if (rpm == null || rpm <= 0f) return 0
        return floor(rpm / STEP).toInt().coerceIn(1, SEGMENTS)
    }

    /** Primeiro segmento cujo início é ≥ [rpm]. */
    fun segmentFor(rpm: Int): Int = ceil(rpm / STEP).toInt()

    /** Cor do segmento [index] pelo giro em que ele começa. */
    fun zone(index: Int): Zone {
        val start = index * STEP
        return when {
            start >= RPM_RED -> Zone.Red
            start >= RPM_WARN -> Zone.Warn
            else -> Zone.Normal
        }
    }

    fun shiftLight(rpm: Float?): Boolean = rpm != null && rpm >= RPM_SHIFT

    fun speedText(speed: Float?): String = speed?.roundToInt()?.coerceIn(0, 999)?.toString() ?: DASH

    /** Arredonda a dezenas: o valor cru oscila a cada leitura e o número ficaria tremendo. */
    fun rpmText(rpm: Float?): String = rpm?.let { ((it / 10f).roundToInt() * 10).coerceAtLeast(0).toString() } ?: DASH

    /** Odômetro total em km com ponto de milhar; ≤ 0 = CAN sem dado (carro rodado nunca marca 0). */
    fun odoText(km: Int?): String = km?.takeIf { it > 0 }?.let { String.format(ptBr, "%,d", it) } ?: DASH

    /** Uma casa decimal com vírgula (bateria, combustível, temperatura). */
    fun valueText(value: Float?): String = value?.let { String.format(ptBr, "%.1f", it) } ?: DASH

    /** O que o Painel mostra, já com a origem decidida. `null` = sem dado ("—"). */
    data class Reading(
        val speed: Float?,
        val rpm: Float?,
        val fuelLiters: Float?,
        val outsideTempC: Float?,
        val odoKm: Int?,
        val batteryVoltage: Float?,
        val handbrakeOn: Boolean?,
    )

    /**
     * Na central do Fox (testado em 2026-11-07) o ICar vem vazio e o CAN bus tem os dados, então o CAN
     * vem primeiro e o ICar fica de reserva. `Int.MIN_VALUE` é "sem dado" no CAN.
     * - Velocidade: o app CAN descarta o 0 enquanto ele nunca mudou (`CanPopWind.isSpeedValid`); com
     *   RPM chegando, velocidade ausente é 0.
     * - Temperatura externa: o CAN entrega °C + 40 (74,5 contra 34,5 na tela original, ao mesmo tempo).
     * - Bateria: o DashBoard do CAN não tem; só o ICar.
     * - Freio de mão: indisponível. O do ICar vem do fio do chicote (GPIO), não validado no carro.
     */
    fun reading(car: CarState, can: DashBoard?): Reading {
        val canRpm = can?.rotationRate?.takeIf { it != Int.MIN_VALUE && it >= 0 }?.toFloat()
        val canSpeed = can?.speed?.takeIf { it != Int.MIN_VALUE && it >= 0 }?.toFloat()
            ?: if (canRpm != null) 0f else null
        val canFuel = can?.fuel?.takeIf { it.isCanValue() && it >= 0f }
        val canTemp = can?.ambientTemp?.takeIf { it.isCanValue() }?.let { it - CAN_TEMP_OFFSET }
        return Reading(
            speed = canSpeed ?: car.speed,
            rpm = canRpm ?: car.rpm,
            fuelLiters = canFuel ?: car.fuelLiters,
            outsideTempC = canTemp ?: car.outsideTempC,
            odoKm = can?.odo?.takeIf { it > 0 },
            batteryVoltage = car.batteryVoltage,
            handbrakeOn = null,
        )
    }

    private const val CAN_TEMP_OFFSET = 40f

    private fun Float.isCanValue() = !isNaN() && this != Int.MIN_VALUE.toFloat()
}
