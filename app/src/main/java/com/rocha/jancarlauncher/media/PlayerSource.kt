package com.rocha.jancarlauncher.media

import com.rocha.jancarlauncher.launch.AppTarget
import com.rocha.jancarlauncher.launch.AppTargets
import com.rocha.jancarlauncher.launch.Kind

/**
 * Que app o toque no texto do player abre: o da **fonte** do que toca. Música da Jancar (tipo 0) → Música; rádio
 * (1) → Rádio; Bluetooth da central (A2DP, 7) → Música BT; sessão padrão → o app dela (pelo pacote). Outros tipos do
 * serviço da central (terceiros espelhados, CarPlay, vídeo, AUX) → nada: abrir a Música trocaria a fonte de áudio.
 */
object PlayerSource {
    private const val A2DP_TYPE = 7

    fun target(source: NowPlayingSource, jancarType: Int?, sessionPkg: String?): AppTarget? = when (source) {
        NowPlayingSource.Jancar -> when (jancarType) {
            0 -> AppTargets.MUSIC
            JancarNowPlaying.RADIO_TYPE -> AppTargets.RADIO
            A2DP_TYPE -> AppTargets.BT_MUSIC
            else -> null
        }
        NowPlayingSource.Session -> sessionPkg?.let { AppTarget("session", "App de música", Kind.App, it, null) }
    }
}
