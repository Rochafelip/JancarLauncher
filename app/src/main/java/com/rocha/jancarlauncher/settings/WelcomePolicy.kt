package com.rocha.jancarlauncher.settings

/** Quando mostrar a tela de Boas-vindas. A central acorda de suspensão ao ligar o carro (não reinicia). */
object WelcomePolicy {
    /** Tela apagada por pelo menos isto → mostra a Boas-vindas ao acordar (padrão meu: 10 min). */
    const val WAKE_THRESHOLD_MS = 10 * 60_000L

    /** Primeira abertura depois de um reinício do aparelho (ou primeira execução do app). */
    fun shouldShowOnLaunch(bootNow: Int, bootSaved: Int?): Boolean = bootSaved == null || bootNow != bootSaved

    /** Ao acordar da suspensão: só se sabemos quando a tela apagou e passou o limite. */
    fun shouldShowOnWake(screenOffAtMs: Long?, nowMs: Long, thresholdMs: Long = WAKE_THRESHOLD_MS): Boolean =
        screenOffAtMs != null && nowMs - screenOffAtMs >= thresholdMs
}
