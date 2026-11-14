package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.ui.dash.ConsumptionText
import com.rocha.jancarlauncher.ui.dash.DashModel

/**
 * Regras da faixa do Painel sobre o Android Auto (pedido do dono, 2026-11-13). Puras, testáveis.
 * O AA não pode ser embutido no launcher (só apps do sistema podem); então o Painel vai por cima dele.
 */
object OverlayPolicy {
    /** Apps de projeção sobre os quais a faixa aparece (pacotes confirmados em `launch/AppTargets`). */
    val TARGETS = setOf("com.google.android.projection.sink", "com.autochips.carplayapp")

    fun shouldShow(foreground: String?, canDraw: Boolean, extraTarget: String? = null): Boolean =
        canDraw && foreground != null && (foreground in TARGETS || foreground == extraTarget)

    /** O que a faixa mostra; `null` em cada linha = sem dado (a linha some). Tudo `null` = faixa some. */
    data class Lines(
        val speed: String?,
        val rpm: String?,
        /** Giro em número, para a barrinha do cartão. */
        val rpmValue: Float?,
        val stage: DashModel.ShiftStage,
        val consumption: String?,
    ) {
        val isEmpty: Boolean get() = speed == null && rpm == null && consumption == null
    }

    fun lines(r: DashModel.Reading, live: ObdClient.Live, mode: DashModel.DriveMode): Lines {
        val (now, unit) = ConsumptionText.instant(live)
        return Lines(
            speed = r.speed?.let { DashModel.speedText(it) },
            rpm = r.rpm?.let { DashModel.rpmText(it) },
            rpmValue = r.rpm,
            stage = DashModel.shiftStage(r.rpm, mode),
            consumption = if (now == DashModel.DASH) null else "$now $unit",
        )
    }
}
