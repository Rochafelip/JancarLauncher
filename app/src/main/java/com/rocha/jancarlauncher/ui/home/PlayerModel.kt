package com.rocha.jancarlauncher.ui.home

import com.rocha.jancarlauncher.media.NowPlayingState

enum class PlayerMode { Track, Radio, Idle }

/** O que o player da Home mostra: modo, título, subtítulo (`null` = some) e se está tocando. */
data class PlayerView(val mode: PlayerMode, val title: String, val subtitle: String?, val playing: Boolean)

/** Regra do player (todos os temas): rádio da central, faixa (Jancar ou sessão padrão) ou "Nada tocando". */
object PlayerModel {
    fun of(np: NowPlayingState): PlayerView {
        val title = np.title?.takeIf { it.isNotBlank() }?.trim()
        val artist = np.artist?.takeIf { it.isNotBlank() }?.trim()
        return when {
            np.isRadio -> {
                val t = title ?: "Rádio"
                val sub = when {
                    artist != null -> "Rádio · $artist"
                    title != null -> "Rádio"
                    else -> null
                }
                PlayerView(PlayerMode.Radio, t, sub, np.isPlaying)
            }
            title == null && artist == null -> PlayerView(PlayerMode.Idle, "Nada tocando", null, false)
            else -> PlayerView(PlayerMode.Track, title ?: "Faixa sem nome", artist, np.isPlaying)
        }
    }
}
