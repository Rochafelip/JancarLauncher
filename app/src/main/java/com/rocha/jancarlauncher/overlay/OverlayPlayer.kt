package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.media.NowPlayingSource
import com.rocha.jancarlauncher.media.NowPlayingState
import com.rocha.jancarlauncher.ui.home.PlayerMode
import com.rocha.jancarlauncher.ui.home.PlayerModel

/** Por onde o comando do player sobre o AA vai. */
enum class MediaRoute {
    /** Música/Rádio da central: serviço de mídia da Jancar (mesmo caminho dos botões do volante). */
    Jancar,
    /** Sessão padrão que o launcher enxerga (Bluetooth, Spotify…). */
    Session,
    /** Nada visível (música do celular pelo Android Auto): tecla de mídia do Android, vai para quem toca. */
    MediaKey,
}

/** Retângulo de toque de um botão (px, coordenadas do cartão). */
data class HitBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Regras puras do player no cartão sobre o Android Auto (⏮ ⏯ ⏭ + nome da música). */
object OverlayPlayer {
    fun route(source: NowPlayingSource?, np: NowPlayingState): MediaRoute = when {
        PlayerModel.of(np).mode == PlayerMode.Idle -> MediaRoute.MediaKey
        source == NowPlayingSource.Jancar -> MediaRoute.Jancar
        source == NowPlayingSource.Session -> MediaRoute.Session
        else -> MediaRoute.MediaKey
    }

    /** "Título · Artista" (ou "FM 99.1 · Rádio"); `null` = nada que o launcher veja tocando (a linha some). */
    fun trackLine(np: NowPlayingState): String? {
        val v = PlayerModel.of(np)
        return if (v.mode == PlayerMode.Idle) null else listOfNotNull(v.title, v.subtitle).joinToString(" · ")
    }

    /** Índice do botão sob (x, y), bordas inclusive; `null` = fora dos botões. */
    fun hit(x: Float, y: Float, boxes: List<HitBox>): Int? =
        boxes.indexOfFirst { x >= it.left && x <= it.right && y >= it.top && y <= it.bottom }.takeIf { it >= 0 }
}
