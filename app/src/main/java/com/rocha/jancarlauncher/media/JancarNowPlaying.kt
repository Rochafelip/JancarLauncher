package com.rocha.jancarlauncher.media

/** De onde vem o que o card "Tocando agora" mostra (e para onde vão os botões de transporte). */
enum class NowPlayingSource { Session, Jancar }

/**
 * Regras do "Tocando agora" para a Música da Jancar (serviço IMedia). Valores calibrados na central:
 * `onMediaChange` → text1 = título (nome do arquivo quando a faixa não tem tag), text2 = artista,
 * num1×num2 = tamanho da capa em pixels ARGB crus (4 bytes/pixel); `onPlayStateChange` → b = estado
 * (3 = tocando, como `PlaybackState.STATE_PLAYING`), c = posição ms, d = duração ms.
 */
object JancarNowPlaying {
    private const val STATE_PLAYING = 3
    private val audioExtension = Regex("""\.(mp3|flac|wav|aac|m4a|ogg|wma|ape|opus)$""", RegexOption.IGNORE_CASE)

    /** [artPng] = capa já convertida para PNG (a conversão usa Bitmap, fica fora daqui). */
    fun map(info: JancarMediaInfo?, play: JancarPlayState?, artPng: ByteArray?): NowPlayingState {
        if (info == null) return NowPlayingState.EMPTY
        return NowPlayingState(
            title = info.text1?.trim()?.replace(audioExtension, "")?.takeIf { it.isNotBlank() },
            artist = info.text2?.trim()?.takeIf { it.isNotBlank() },
            art = artPng,
            isPlaying = play?.b == STATE_PLAYING,
            supportsShuffle = false,
        )
    }

    /** A capa só é usável se o tamanho bate com largura × altura × 4 (ARGB_8888 cru). */
    fun isRawArgb(art: ByteArray?, width: Int, height: Int): Boolean =
        art != null && width > 0 && height > 0 && art.size == width * height * 4

    /**
     * Quem está tocando ganha (Jancar primeiro); se nada toca, mostra a Música da Jancar pausada se ela tiver
     * faixa — é o player nativo da central —, senão a sessão padrão (Bluetooth, apps de terceiros).
     */
    fun pick(session: NowPlayingState, jancar: NowPlayingState): NowPlayingSource = when {
        jancar.isPlaying -> NowPlayingSource.Jancar
        session.isPlaying -> NowPlayingSource.Session
        jancar.title != null || jancar.artist != null -> NowPlayingSource.Jancar
        else -> NowPlayingSource.Session
    }
}
