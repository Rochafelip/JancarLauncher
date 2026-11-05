package com.rocha.jancarlauncher.media

/** Dados mínimos de uma sessão de mídia ativa, sem tipos do Android — testável em JVM puro. */
data class SessionInfo(
    val id: String,
    val isPlaying: Boolean,
    val hasMetadata: Boolean,
    val lastActiveMs: Long,
)

/** Escolhe qual sessão de mídia mostrar no card "Tocando agora". */
object NowPlayingSessionPicker {

    /**
     * Prioriza a sessão tocando mais recente; sem nenhuma tocando, a última com metadata
     * (pausada, mas identificável); sem nenhuma, null (card some).
     */
    fun pick(sessions: List<SessionInfo>): String? {
        val playing = sessions.filter { it.isPlaying }
        if (playing.isNotEmpty()) return playing.maxBy { it.lastActiveMs }.id

        val withMetadata = sessions.filter { it.hasMetadata }
        if (withMetadata.isNotEmpty()) return withMetadata.maxBy { it.lastActiveMs }.id

        return null
    }
}
