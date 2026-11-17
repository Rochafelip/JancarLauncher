package com.rocha.jancarlauncher.media

/** Última fonte que estava tocando (gravada ao tocar), para retomar ao ligar a central. */
sealed interface LastSource {
    data object Jancar : LastSource
    data class Session(val pkg: String) : LastSource

    fun encode(): String = when (this) {
        Jancar -> "jancar"
        is Session -> "session:$pkg"
    }

    companion object {
        fun decode(s: String?): LastSource? = when {
            s == "jancar" -> Jancar
            s != null && s.startsWith("session:") && s.length > 8 -> Session(s.removePrefix("session:"))
            else -> null
        }
    }
}

/**
 * Retomar a música ao ligar (Ajustes; desligado por padrão até o teste no carro): só se o dono ligou, há uma última
 * fonte, nada toca agora e o Android Auto/CarPlay não está na frente (aí quem decide é o celular).
 */
object MusicResume {
    private val PROJECTION = setOf("com.google.android.projection.sink", "com.autochips.carplayapp")

    fun shouldResume(enabled: Boolean, last: LastSource?, foregroundPkg: String?, playingNow: Boolean): Boolean =
        enabled && last != null && !playingNow && foregroundPkg !in PROJECTION

    /** Espera depois da Boas-vindas aparecer (a central ainda está acordando os serviços de mídia). */
    const val DELAY_MS = 4_000L
}
