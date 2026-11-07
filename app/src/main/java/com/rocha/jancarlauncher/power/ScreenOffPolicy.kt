package com.rocha.jancarlauncher.power

import android.view.KeyEvent
import com.rocha.jancarlauncher.ui.home.BrightnessMapper
import com.rocha.jancarlauncher.ui.nav.Screen

/**
 * Regras do botão "Apagar tela". Sem root nem Device Admin, a tela não é desligada de verdade (o toque não a
 * acenderia): o launcher cobre tudo de preto com o brilho da janela no mínimo e mantém a central acordada.
 * O toque/tecla seguinte acorda para a Boas-vindas. Só a janela do launcher muda — nunca o brilho do sistema.
 */
object ScreenOffPolicy {
    /** `WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF`: mínimo absoluto (ignora o piso de 10% do slider). */
    const val WINDOW_BRIGHTNESS_OFF = 0f

    /** `WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE`: a janela segue o brilho do sistema. */
    const val WINDOW_BRIGHTNESS_NONE = -1f

    /** Brilho da janela para [screen]; [userPercent] null = o usuário nunca escolheu brilho no launcher. */
    fun windowBrightness(screen: Screen, userPercent: Int?): Float = when {
        screen == Screen.Off -> WINDOW_BRIGHTNESS_OFF
        userPercent == null -> WINDOW_BRIGHTNESS_NONE
        else -> BrightnessMapper.toWindow(userPercent)
    }

    /** Mudanças de brilho (slider, modo noturno) ficam só gravadas enquanto a tela está apagada. */
    fun shouldApplyBrightness(screen: Screen): Boolean = screen != Screen.Off

    /** Toque/tecla com a tela apagada → Boas-vindas (o toque que acorda não aciona mais nada). */
    fun afterWake(screen: Screen): Screen = if (screen == Screen.Off) Screen.Welcome else screen

    /** Volume e mídia não acordam: passam direto (girar o volume com a tela apagada só muda o volume). */
    private val passThroughKeys = setOf(
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
    )

    /** Se a tecla física [keyCode] acorda a tela apagada (as demais teclas — D-pad, OK, voltar — acordam). */
    fun keyWakes(keyCode: Int): Boolean = keyCode !in passThroughKeys

    /** Outro app passou à frente com a tela apagada → ao voltar, Boas-vindas em vez da tela preta. */
    fun afterLeavingForeground(screen: Screen): Screen = afterWake(screen)
}
