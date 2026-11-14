package com.rocha.jancarlauncher

import com.rocha.jancarlauncher.obd.FuelModel
import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.GearModel

/**
 * Roteiro do modo DEMO da tela do Fox (só debug; pedido do dono, 2026-11-14: "um demo para essa página funcionar
 * tudo que já estava funcionando"). Dados **inventados** para exercitar a tela sem o carro: a prévia mostra a
 * etiqueta DEMO em cima, e nada disto existe no build que vai para a central.
 *
 * Ciclo de [CYCLE_S] s: parado com a porta aberta → fecha e acende o farol → acelera trocando marcha (giro sobe e
 * cai a cada troca) → seta esquerda em velocidade de cruzeiro → freia com seta direita → pisca-alerta → parado,
 * porta-malas abre. Água do motor esquentando até ~90 °C; combustível descendo devagar; consumo e trip acompanhando. Giro pelas relações reais do câmbio
 * ([GearModel]): acelerando troca logo depois do ponto do modo (shift light e ↑); freando segura a 5ª e o giro cai (aparece ↓4).
 */
object FoxDemo {
    const val CYCLE_S = 32.0

    data class Frame(val reading: DashModel.Reading, val obd: ObdClient.Live)

    private val ratios = GearModel.RPM_PER_KMH

    /** Acelerando: a marcha mais baixa em que o giro não passa de [shiftAt] (troca logo depois do ponto do modo). */
    fun gearAccelerating(speed: Float, shiftAt: Float = 3100f): Int =
        ratios.indices.first { it == ratios.lastIndex || speed * ratios[it] <= shiftAt }

    fun rpm(speed: Float, braking: Boolean = false, shiftAt: Float = 3100f): Float {
        if (speed < 8f) return 850f // embreagem pisada / parado: marcha lenta
        // Freando: segura a 5ª (o giro cai e aparece ↓4) e, abaixo de ~1000 rpm, pisa a embreagem até parar.
        if (braking) return (speed * ratios.last()).toFloat().takeIf { it >= 1000f } ?: 850f
        return maxOf(850f, (speed * ratios[gearAccelerating(speed, shiftAt)]).toFloat())
    }

    fun speed(t: Double): Float = when {
        t < 3 -> 0f
        t < 15 -> (75 * ease((t - 3) / 12)).toFloat()
        t < 22 -> 75f
        t < 28 -> (75 * (1 - ease((t - 22) / 6))).toFloat()
        else -> 0f
    }

    /** Quadro em [seconds]; acelerando, troca 100 rpm depois do ponto do [mode] (mostra o shift light piscando). */
    fun at(seconds: Double, mode: DashModel.DriveMode = DashModel.DriveMode.Daily): Frame {
        val t = seconds % CYCLE_S
        val v = speed(t)
        val rpm = rpm(v, braking = t in 22.0..28.0, shiftAt = mode.shiftRpm + 100f)
        val accelerating = t in 3.0..15.0
        val turnLeft = t in 15.0..19.0 || t in 26.0..28.0
        val turnRight = t in 22.0..25.0 || t in 26.0..28.0
        val reading = DashModel.Reading(
            speed = v,
            rpm = rpm,
            fuelLiters = (32.0 - seconds * 0.01).coerceAtLeast(5.0).toFloat(),
            outsideTempC = 33f,
            odoKm = 68201 + (seconds / CYCLE_S).toInt(),
            doors = DashModel.Doors(
                frontLeft = t < 2, frontRight = false, rearLeft = false, rearRight = false,
                trunk = t in 29.0..31.0, hood = null,
            ),
            headlightOn = t >= 2.5,
            turnLeft = turnLeft,
            turnRight = turnRight,
        )
        // Gasto: marcha lenta ~0,7 L/h, sobe com o giro e mais ainda acelerando (estimativa só para o demo).
        val lph = 0.7f + rpm / 1000f * (if (accelerating) 1.9f else 1.1f) - 0.8f
        val obd = ObdClient.Live(
            ObdClient.Status.Connected, "DEMO", speedKmh = v,
            litersPerHour = lph.coerceAtLeast(0.7f),
            kmPerLiter = FuelModel.kmPerLiter(v, lph.coerceAtLeast(0.7f)),
            // Água: motor frio esquentando até ~90 °C no primeiro minuto e parando ali (termostato).
            coolantC = (45.0 + 45.0 * (1 - Math.exp(-seconds / 20.0))).toFloat(),
        )
        return Frame(reading, obd)
    }

    private fun ease(x: Double): Double { val c = x.coerceIn(0.0, 1.0); return c * c * (3 - 2 * c) }
}
