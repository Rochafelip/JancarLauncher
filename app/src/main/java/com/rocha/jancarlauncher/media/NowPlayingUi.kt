package com.rocha.jancarlauncher.media

/** Estado exibido no card "Tocando agora". Sem capa: [art] null → ícone de nota musical na UI. */
data class NowPlayingState(
    val title: String?,
    val artist: String?,
    val art: ByteArray?,
    val isPlaying: Boolean,
    val supportsShuffle: Boolean,
) {
    companion object {
        val EMPTY = NowPlayingState(title = null, artist = null, art = null, isPlaying = false, supportsShuffle = false)
    }

    // ByteArray não tem equals/hashCode estrutural por padrão; geramos pra permitir comparação em teste.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NowPlayingState) return false
        return title == other.title && artist == other.artist && art.contentEquals(other.art) &&
            isPlaying == other.isPlaying && supportsShuffle == other.supportsShuffle
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (art?.contentHashCode() ?: 0)
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + supportsShuffle.hashCode()
        return result
    }
}

/**
 * Converte metadados já extraídos da sessão (nunca os tipos reais de `MediaMetadata`/`PlaybackState`,
 * que não mockam bem em testes de JVM) no estado de UI.
 */
object NowPlayingMapper {
    fun map(title: String?, artist: String?, art: ByteArray?, playbackState: Int, actions: Long): NowPlayingState =
        NowPlayingState(
            title = title,
            artist = artist,
            art = art,
            isPlaying = playbackState == STATE_PLAYING,
            supportsShuffle = actions and ACTION_SET_SHUFFLE_MODE != 0L,
        )

    // Espelham android.media.session.PlaybackState — evita importar o tipo real aqui.
    const val STATE_PLAYING = 3
    const val ACTION_SET_SHUFFLE_MODE = 1L shl 15
}
