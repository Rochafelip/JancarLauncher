package com.rocha.jancarlauncher.settings

import java.time.LocalTime

/** Decide a ação de modo noturno a tomar, sem tocar em Android (testável em JVM puro). */
object NightModeController {

    sealed class Action {
        /** Entrando na janela noturna: aplicar escuro + [NightModePolicy.NIGHT_BRIGHTNESS], guardando o estado anterior. */
        data class ApplyNight(val previousDark: Boolean, val previousBrightness: Int?) : Action()
        /** Saindo da janela noturna: restaurar o tema/brilho de antes da última entrada. */
        data class RestoreDay(val dark: Boolean, val brightness: Int?) : Action()
        /** Nada a fazer (já no estado certo, ou o usuário mudou o tema manualmente dentro da janela). */
        object NoOp : Action()
    }

    /**
     * @param now horário atual.
     * @param wasApplied se a automação está com o estado noturno aplicado agora (persistido).
     * @param currentDark tema atual.
     * @param currentBrightness brilho atual escolhido pelo usuário (null = nunca escolheu).
     * @param previousDark tema salvo de antes da última entrada na janela (só relevante se [wasApplied]).
     * @param previousBrightness brilho salvo de antes da última entrada (só relevante se [wasApplied]).
     */
    fun evaluate(
        now: LocalTime,
        wasApplied: Boolean,
        currentDark: Boolean,
        currentBrightness: Int?,
        previousDark: Boolean,
        previousBrightness: Int?,
    ): Action {
        val isNight = NightModePolicy.isNight(now)
        return when {
            isNight && !wasApplied -> Action.ApplyNight(previousDark = currentDark, previousBrightness = currentBrightness)
            !isNight && wasApplied -> Action.RestoreDay(dark = previousDark, brightness = previousBrightness)
            else -> Action.NoOp
        }
    }
}
