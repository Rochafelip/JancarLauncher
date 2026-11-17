package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.obd.ObdClient
import com.rocha.jancarlauncher.obd.SpeedAlertRule
import com.rocha.jancarlauncher.ui.dash.ConsumptionText
import com.rocha.jancarlauncher.ui.dash.DashModel
import com.rocha.jancarlauncher.ui.dash.GearModel

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
        /** Marcha engatada (GearModel) e a seta da sugestão ("↑4", "↓3"); `null` = some. */
        val gear: String? = null,
        val hint: String? = null,
        val hintDown: Boolean = false,
        /** Aviso grave em vermelho (motor esquentando: "ÁGUA 112 °C"); `null` = some. */
        val warning: String? = null,
        /** Player embaixo (⏮ ⏯ ⏭): sempre que o cartão aparece e há um player ligado. */
        val showPlayer: Boolean = false,
        /** "Título · Artista" do que toca; `null` = a linha some (ex.: música do celular pelo AA). */
        val track: String? = null,
        /** ⏯ mostra pausar (tocando) ou tocar; `null` = não se sabe (mostra play/pausa). */
        val playing: Boolean? = null,
        /** Velocidade acima do limite do aviso sonoro (número em vermelho). */
        val speedOver: Boolean = false,
    ) {
        val isEmpty: Boolean get() = speed == null && rpm == null && consumption == null && warning == null
    }

    fun lines(
        r: DashModel.Reading, live: ObdClient.Live, mode: DashModel.DriveMode, gear: Int? = null,
        health: DashModel.Health = DashModel.Health(),
        limit: Int? = null,
        player: com.rocha.jancarlauncher.media.NowPlayingState? = null,
    ): Lines {
        val (now, unit) = ConsumptionText.instant(live)
        val s = GearModel.suggest(gear, r.rpm, r.speed, mode)
        return Lines(
            speed = r.speed?.let { DashModel.speedText(it) },
            rpm = r.rpm?.let { DashModel.rpmText(it) },
            rpmValue = r.rpm,
            stage = DashModel.shiftStage(r.rpm, mode),
            consumption = if (now == DashModel.DASH) null else "$now $unit",
            gear = gear?.toString(),
            hint = GearModel.hintText(s),
            hintDown = s.hint == GearModel.Hint.Down,
            warning = health.coolantC?.takeIf { health.overheat }?.let { "ÁGUA ${it.toInt()} °C" },
            speedOver = SpeedAlertRule.over(r.speed, limit),
            showPlayer = player != null,
            track = player?.let { OverlayPlayer.trackLine(it) },
            playing = player?.takeIf { OverlayPlayer.trackLine(it) != null }?.isPlaying,
        )
    }
}
