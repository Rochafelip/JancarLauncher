package com.rocha.jancarlauncher.media

/** Comandos de transporte de uma fonte. */
class Transport(val play: () -> Unit, val pause: () -> Unit, val next: () -> Unit, val previous: () -> Unit)

/**
 * O que a Home sabe da mídia (faixa mostrada, fonte e os comandos de cada uma), publicado pela `AppRoot` para quem
 * não é Compose: o cartão sobre o Android Auto (`overlay/AutoOverlay`). A `AppRoot` continua viva com o AA na
 * frente (a activity só para, não é destruída), então o estado segue atualizado.
 */
class MediaBridge {
    @Volatile var state: NowPlayingState = NowPlayingState.EMPTY
    @Volatile var source: NowPlayingSource? = null
    @Volatile var jancar: Transport? = null
    @Volatile var session: Transport? = null
}
