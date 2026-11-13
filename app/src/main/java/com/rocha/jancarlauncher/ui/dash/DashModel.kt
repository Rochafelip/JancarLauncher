package com.rocha.jancarlauncher.ui.dash

import com.rocha.jancarlauncher.can.Cabin
import com.rocha.jancarlauncher.can.CarState
import com.rocha.jancarlauncher.can.DashBoard
import com.rocha.jancarlauncher.can.Lights
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

    /**
     * Modo de condução, trocado por um botão no Painel: define o ponto de troca de marcha. Valores do
     * motorista (2026-11-10): troca por volta de 3000 rpm no dia a dia e 5000 andando bravo.
     * Só o modo bravo mostra a faixa amarela "SHIFT"; o dia a dia avisa só com cor.
     */
    enum class DriveMode(val label: String, val shiftRpm: Int, val shiftBanner: Boolean) {
        Daily("DIA A DIA", 3000, shiftBanner = false),
        Sport("BRAVO", 5000, shiftBanner = true);

        fun next(): DriveMode = entries[(ordinal + 1) % entries.size]

        companion object {
            /** Modo salvo nas preferências; ausente ou desconhecido = dia a dia. */
            fun fromName(name: String?): DriveMode = entries.firstOrNull { it.name == name } ?: Daily
        }
    }

    /** Quantos rpm antes do ponto de troca a barra e o número ficam âmbar ("chegando perto"). */
    const val SHIFT_APPROACH_RPM = 500

    enum class ShiftStage { None, Near, Shift }

    fun shiftStage(rpm: Float?, mode: DriveMode): ShiftStage = when {
        rpm == null -> ShiftStage.None
        rpm >= mode.shiftRpm -> ShiftStage.Shift
        rpm >= mode.shiftRpm - SHIFT_APPROACH_RPM -> ShiftStage.Near
        else -> ShiftStage.None
    }

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

    /** Faixa do segmento [index] pelo giro em que ele começa, relativa ao ponto de troca do [mode]. */
    fun zone(index: Int, mode: DriveMode): Zone {
        val start = index * STEP
        return when {
            start >= mode.shiftRpm -> Zone.Red
            start >= mode.shiftRpm - SHIFT_APPROACH_RPM -> Zone.Warn
            else -> Zone.Normal
        }
    }

    fun speedText(speed: Float?): String = speed?.roundToInt()?.coerceIn(0, 999)?.toString() ?: DASH

    /** Arredonda a dezenas: o valor cru oscila a cada leitura e o número ficaria tremendo. */
    fun rpmText(rpm: Float?): String = rpm?.let { ((it / 10f).roundToInt() * 10).coerceAtLeast(0).toString() } ?: DASH

    /** Odômetro total em km com ponto de milhar; ≤ 0 = CAN sem dado (carro rodado nunca marca 0). */
    fun odoText(km: Int?): String = km?.takeIf { it > 0 }?.let { String.format(ptBr, "%,d", it) } ?: DASH

    /** Uma casa decimal com vírgula (combustível, temperatura). */
    fun valueText(value: Float?): String = value?.let { String.format(ptBr, "%.1f", it) } ?: DASH

    /** Aberturas do carro: `true` = aberta, `false` = fechada, `null` = sem dado. */
    data class Doors(
        val frontLeft: Boolean? = null,
        val frontRight: Boolean? = null,
        val rearLeft: Boolean? = null,
        val rearRight: Boolean? = null,
        val trunk: Boolean? = null,
        val hood: Boolean? = null,
    ) {
        private val all get() = listOf(frontLeft, frontRight, rearLeft, rearRight, trunk, hood)
        val anyOpen: Boolean get() = all.any { it == true }
        val known: Boolean get() = all.any { it != null }
    }

    /** O que o Painel mostra, já com a origem decidida. `null` = sem dado ("—"). */
    data class Reading(
        val speed: Float?,
        val rpm: Float?,
        val fuelLiters: Float?,
        val outsideTempC: Float?,
        val odoKm: Int?,
        val doors: Doors = Doors(),
        /** Fio de iluminação do chicote (GPIO, via ICar); validado no Fox. */
        val headlightOn: Boolean? = null,
        val turnLeft: Boolean? = null,
        val turnRight: Boolean? = null,
    )

    /**
     * Na central do Fox (testado em 2026-11-07) o ICar vem vazio e o CAN bus tem os dados, então o CAN
     * vem primeiro e o ICar fica de reserva. `Int.MIN_VALUE` é "sem dado" no CAN.
     * - Velocidade: o app CAN descarta o 0 enquanto ele nunca mudou (`CanPopWind.isSpeedValid`); com
     *   RPM chegando, velocidade ausente é 0.
     * - Temperatura externa: o CAN entrega °C + 40 (74,5 contra 34,5 na tela original, ao mesmo tempo).
     * - Portas/porta-malas: `Cabin` do CAN (1 = aberta; testado: porta do motorista = 1). O protocolo PQ
     *   não manda o capô nem o cinto (o cinto fica sempre no valor padrão 1, por isso fica de fora).
     * - Setas/pisca-alerta: `Lights` do CAN (testado no Fox em 2026-11-13). Vem a posição da alavanca,
     *   fixa enquanto ligada (não pisca); o pisca-alerta chega como as duas setas = 1 e o campo
     *   `emergencyFlasher` nunca vem (fica Int.MIN_VALUE).
     * - Farol: fio do chicote lido pelo ICar (o CAN não expõe).
     * - Freio de mão e bateria: fora do Painel. O CAN não expõe nenhum dos dois, e o fio do freio de mão
     *   não está ligado nesta instalação (testado em 2026-11-07: nunca mudou).
     */
    fun reading(car: CarState, can: DashBoard?, cabin: Cabin? = null, lights: Lights? = null): Reading {
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
            doors = Doors(
                frontLeft = cabin?.flDoorStatus.flag() ?: car.isDoorOpen(DOOR_FRONT_LEFT),
                frontRight = cabin?.frDoorStatus.flag() ?: car.isDoorOpen(DOOR_FRONT_RIGHT),
                rearLeft = cabin?.rlDoorStatus.flag() ?: car.isDoorOpen(DOOR_REAR_LEFT),
                rearRight = cabin?.rrDoorStatus.flag() ?: car.isDoorOpen(DOOR_REAR_RIGHT),
                trunk = cabin?.trunkStatus.flag() ?: car.isDoorOpen(DOOR_TRUNK),
                hood = cabin?.bonnetStatus.flag() ?: car.isDoorOpen(DOOR_HOOD),
            ),
            headlightOn = car.headlightOn,
            turnLeft = turn(lights?.leftDirectionIndicator, lights?.emergencyFlasher)
                ?: turn(car.isLightOn(LIGHT_TURN_LEFT), car.isLightOn(LIGHT_EMERGENCY)),
            turnRight = turn(lights?.rightDirectionIndicator, lights?.emergencyFlasher)
                ?: turn(car.isLightOn(LIGHT_TURN_RIGHT), car.isLightOn(LIGHT_EMERGENCY)),
        )
    }

    /** Seta acesa pela própria seta ou pelo pisca-alerta; `null` só se nenhum dos dois chegou. */
    private fun turn(signal: Int?, hazard: Int?): Boolean? = turn(signal.flag(), hazard.flag())

    private fun turn(signal: Boolean?, hazard: Boolean?): Boolean? =
        if (signal == null && hazard == null) null else signal == true || hazard == true

    /** Valor do CAN: 1 = ligado/aberto, 0 = desligado/fechado, qualquer outro (Int.MIN_VALUE) = sem dado. */
    private fun Int?.flag(): Boolean? = when (this) {
        1 -> true
        0 -> false
        else -> null
    }

    // Índices do ICar (CarState.isDoorOpen / isLightOn).
    private const val DOOR_FRONT_LEFT = 0
    private const val DOOR_FRONT_RIGHT = 1
    private const val DOOR_REAR_LEFT = 2
    private const val DOOR_REAR_RIGHT = 3
    private const val DOOR_HOOD = 4
    private const val DOOR_TRUNK = 6
    private const val LIGHT_TURN_LEFT = 0
    private const val LIGHT_TURN_RIGHT = 1
    private const val LIGHT_EMERGENCY = 3

    /** Avisos do Painel; a faixa só aparece quando há algum. [red] = grave (vermelho), senão âmbar. */
    enum class Alert(val red: Boolean) { Shift(true), DoorOpen(true), LowFuel(false) }

    /** Andando acima disto com porta aberta vira aviso vermelho (parado é normal). */
    const val DOOR_ALERT_KMH = 5f

    /** Quais estão abertas, por extenso (detalhe da faixa de aviso). */
    fun openDoorsText(d: Doors): String = listOfNotNull(
        "Motorista".takeIf { d.frontLeft == true },
        "Passageiro".takeIf { d.frontRight == true },
        "Traseira esq.".takeIf { d.rearLeft == true },
        "Traseira dir.".takeIf { d.rearRight == true },
        "Porta-malas".takeIf { d.trunk == true },
        "Capô".takeIf { d.hood == true },
    ).joinToString(", ")

    /** Reserva: a partir daqui avisa (rodar com o tanque no fim força a bomba de combustível). */
    const val LOW_FUEL_LITERS = 7f

    /** Avisos ativos, o mais grave primeiro; lista vazia = tudo bem, nada aparece. */
    fun alerts(r: Reading, mode: DriveMode = DriveMode.Daily): List<Alert> = buildList {
        // Hora de trocar no modo bravo: vem antes de tudo enquanto durar (a tela mostra só ele).
        if (mode.shiftBanner && shiftStage(r.rpm, mode) == ShiftStage.Shift) add(Alert.Shift)
        if (r.doors.anyOpen && r.speed != null && r.speed >= DOOR_ALERT_KMH) add(Alert.DoorOpen)
        // 0 L costuma ser o CAN ainda sem leitura logo após a partida, não tanque vazio.
        if (r.fuelLiters != null && r.fuelLiters > 0f && r.fuelLiters <= LOW_FUEL_LITERS) add(Alert.LowFuel)
    }

    private const val CAN_TEMP_OFFSET = 40f

    private fun Float.isCanValue() = !isNaN() && this != Int.MIN_VALUE.toFloat()
}
