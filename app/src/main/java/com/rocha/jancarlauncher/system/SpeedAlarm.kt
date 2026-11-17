package com.rocha.jancarlauncher.system

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import com.rocha.jancarlauncher.JancarApp
import com.rocha.jancarlauncher.obd.Beep
import com.rocha.jancarlauncher.obd.SpeedAlertRule
import com.rocha.jancarlauncher.ui.dash.DashModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Aviso sonoro acima do limite (escolha do dono: opção A, sem serviço novo). Roda no escopo do app e usa a velocidade
 * que já chega: a do CAN quando a conexão está aberta (Painel, Carro, cartão sobre o AA), senão a do OBD (conectado
 * sempre que o launcher roda). Bipe curto pelo `ToneGenerator`; o fluxo de áudio ainda precisa ser conferido no carro
 * com a música do AA tocando.
 */
class SpeedAlarm(private val app: JancarApp) {
    private val rule = SpeedAlertRule()
    private var tone: ToneGenerator? = null

    /** Limite atual (km/h); `null` = desligado. Lido também pelo cartão sobre o AA. */
    val limit: StateFlow<Int?> = app.settings.speedLimit.stateIn(app.appScope, SharingStarted.Eagerly, null)

    fun start() {
        app.appScope.launch {
            while (true) {
                // Uma volta com erro não pode parar o aviso nem derrubar a HOME: registra e segue.
                runCatching {
                    val beep = rule.update(currentSpeed(), limit.value, SystemClock.elapsedRealtime())
                    if (beep != null) play(beep)
                }.onFailure { app.log.add("velocidade", "erro: ${it.javaClass.simpleName}: ${it.message}") }
                delay(250)
            }
        }
    }

    /** Só prévias de debug: velocidade de mentira (o emulador não tem CAN nem OBD). */
    @Volatile var debugSpeed: Float? = null

    private fun currentSpeed(): Float? {
        debugSpeed?.let { return it }
        val can = app.canBus
        if (can.connected.value) {
            DashModel.reading(app.carService.state.value, can.dashboard.value).speed?.let { return it }
        }
        return app.obd.live.value.speedKmh
    }

    private fun play(beep: Beep) {
        val t = tone ?: runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME) }.getOrNull()?.also { tone = it } ?: return
        if (beep == Beep.Double) app.log.add("velocidade", "acima de ${limit.value} km/h")
        runCatching {
            t.startTone(if (beep == Beep.Double) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 300)
        }
    }

    private companion object {
        const val VOLUME = 80
    }
}
