package com.rocha.jancarlauncher.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JancarNowPlayingTest {
    // Valores reais lidos na central (pendrive tocando "Whatever.mp3", sem tag de artista).
    private val info = JancarMediaInfo(0, "Whatever.mp3", null, 240, 240, null, 43, 49, false)
    private val playing = JancarPlayState(0, 3, 83858, 232248)

    // --- conversão para o card ---

    @Test fun nothingFromService_isEmpty() {
        assertEquals(NowPlayingState.EMPTY, JancarNowPlaying.map(null, null, null))
    }

    @Test fun fileNameTitle_dropsAudioExtension() {
        assertEquals("Whatever", JancarNowPlaying.map(info, playing, null).title)
    }

    @Test fun realTitle_isKeptAsIs() {
        val tagged = info.copy(text1 = "Smells Like Teen Spirit", text2 = "Nirvana")
        val s = JancarNowPlaying.map(tagged, playing, null)
        assertEquals("Smells Like Teen Spirit", s.title)
        assertEquals("Nirvana", s.artist)
    }

    @Test fun titleWithDotsButNotAudioFile_isKept() {
        assertEquals("Mr. Brightside", JancarNowPlaying.map(info.copy(text1 = "Mr. Brightside"), playing, null).title)
    }

    @Test fun blankArtist_isNull() {
        assertNull(JancarNowPlaying.map(info.copy(text2 = "  "), playing, null).artist)
    }

    @Test fun state3_isPlaying_anythingElseIsNot() {
        assertTrue(JancarNowPlaying.map(info, playing, null).isPlaying)
        assertFalse(JancarNowPlaying.map(info, playing.copy(b = 2), null).isPlaying)
        assertFalse(JancarNowPlaying.map(info, null, null).isPlaying)
    }

    @Test fun convertedArt_isPassedThrough() {
        val png = byteArrayOf(1, 2, 3)
        assertTrue(png.contentEquals(JancarNowPlaying.map(info, playing, png).art))
    }

    // --- capa crua (ARGB 4 bytes/pixel) ---

    @Test fun rawArt_isUsableOnlyWhenSizeMatchesWidthTimesHeightTimes4() {
        assertTrue(JancarNowPlaying.isRawArgb(ByteArray(240 * 240 * 4), 240, 240))
        assertFalse(JancarNowPlaying.isRawArgb(ByteArray(100), 240, 240))
        assertFalse(JancarNowPlaying.isRawArgb(null, 240, 240))
        assertFalse(JancarNowPlaying.isRawArgb(ByteArray(0), 0, 0))
    }

    // --- qual fonte o card mostra ---

    private val jancarPlaying = NowPlayingState("Whatever", null, null, isPlaying = true, supportsShuffle = false)
    private val sessionPlaying = NowPlayingState("Song BT", "Artist", null, isPlaying = true, supportsShuffle = false)

    @Test fun jancarPlaying_wins() {
        assertEquals(NowPlayingSource.Jancar, JancarNowPlaying.pick(sessionPlaying.copy(isPlaying = false), jancarPlaying))
    }

    @Test fun sessionPlaying_winsOverPausedJancar() {
        assertEquals(NowPlayingSource.Session, JancarNowPlaying.pick(sessionPlaying, jancarPlaying.copy(isPlaying = false)))
    }

    @Test fun nothingPlaying_pausedJancarWithTitleIsShown() {
        assertEquals(
            NowPlayingSource.Jancar,
            JancarNowPlaying.pick(NowPlayingState.EMPTY, jancarPlaying.copy(isPlaying = false)),
        )
    }

    @Test fun nothingAtAll_fallsBackToSession() {
        assertEquals(NowPlayingSource.Session, JancarNowPlaying.pick(NowPlayingState.EMPTY, NowPlayingState.EMPTY))
    }
}
