package com.rocha.jancarlauncher.ui.home

import com.rocha.jancarlauncher.media.NowPlayingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerModelTest {
    private fun np(title: String?, artist: String?, playing: Boolean = true, radio: Boolean = false) =
        NowPlayingState(title, artist, null, playing, false, isRadio = radio)

    @Test fun empty_isIdle() {
        assertEquals(PlayerView(PlayerMode.Idle, "Nada tocando", null, false), PlayerModel.of(NowPlayingState.EMPTY))
    }

    @Test fun blankTitleAndArtist_isIdle() {
        assertEquals(PlayerMode.Idle, PlayerModel.of(np("  ", null)).mode)
    }

    @Test fun trackWithArtist() {
        assertEquals(PlayerView(PlayerMode.Track, "Ela", "Banda", true), PlayerModel.of(np("Ela", "Banda")))
    }

    @Test fun pausedTrack_isStillTrack() {
        val v = PlayerModel.of(np("Ela", "Banda", playing = false))
        assertEquals(PlayerMode.Track, v.mode)
        assertEquals(false, v.playing)
    }

    @Test fun titleOnly_noSubtitle() {
        assertNull(PlayerModel.of(np("Ela", " ")).subtitle)
    }

    @Test fun artistOnly_isTrackWithoutName() {
        assertEquals(PlayerView(PlayerMode.Track, "Faixa sem nome", "Banda", true), PlayerModel.of(np(null, "Banda")))
    }

    @Test fun radioWithText() {
        assertEquals(PlayerView(PlayerMode.Radio, "FM 99.1", "Rádio", true), PlayerModel.of(np("FM 99.1", null, radio = true)))
    }

    @Test fun radioWithRds() {
        assertEquals("Rádio · Rádio Cidade", PlayerModel.of(np("FM 99.1", "Rádio Cidade", radio = true)).subtitle)
    }

    @Test fun radioWithoutText() {
        assertEquals(PlayerView(PlayerMode.Radio, "Rádio", null, false), PlayerModel.of(np(null, null, playing = false, radio = true)))
    }
}
