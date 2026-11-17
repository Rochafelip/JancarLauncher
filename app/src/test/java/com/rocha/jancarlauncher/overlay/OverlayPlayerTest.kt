package com.rocha.jancarlauncher.overlay

import com.rocha.jancarlauncher.media.NowPlayingSource
import com.rocha.jancarlauncher.media.NowPlayingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayPlayerTest {
    private fun np(title: String?, artist: String? = null, radio: Boolean = false) =
        NowPlayingState(title, artist, null, true, false, isRadio = radio)

    @Test fun route_jancarWithTrack_goesThroughHeadUnitService() {
        assertEquals(MediaRoute.Jancar, OverlayPlayer.route(NowPlayingSource.Jancar, np("Faixa")))
    }

    @Test fun route_sessionWithTrack_goesThroughTheSession() {
        assertEquals(MediaRoute.Session, OverlayPlayer.route(NowPlayingSource.Session, np("Faixa")))
    }

    @Test fun route_nothingVisible_usesTheAndroidMediaKey() {
        // Música do celular pelo Android Auto: o launcher não vê a faixa; a tecla de mídia vai para quem toca.
        assertEquals(MediaRoute.MediaKey, OverlayPlayer.route(NowPlayingSource.Session, NowPlayingState.EMPTY))
        assertEquals(MediaRoute.MediaKey, OverlayPlayer.route(null, NowPlayingState.EMPTY))
    }

    @Test fun trackLine_titleAndArtist_orRadio_orNothing() {
        assertEquals("Ela · Banda", OverlayPlayer.trackLine(np("Ela", "Banda")))
        assertEquals("FM 99.1 · Rádio", OverlayPlayer.trackLine(np("FM 99.1", radio = true)))
        assertNull(OverlayPlayer.trackLine(NowPlayingState.EMPTY))
    }

    @Test fun hit_findsTheButtonUnderTheFinger() {
        val buttons = listOf(HitBox(16f, 200f, 64f, 248f), HitBox(70f, 200f, 118f, 248f), HitBox(124f, 200f, 172f, 248f))
        assertEquals(0, OverlayPlayer.hit(20f, 210f, buttons))
        assertEquals(1, OverlayPlayer.hit(90f, 247f, buttons))
        assertEquals(2, OverlayPlayer.hit(172f, 200f, buttons))
        assertNull(OverlayPlayer.hit(66f, 220f, buttons)) // no vão entre botões
        assertNull(OverlayPlayer.hit(90f, 100f, buttons)) // na velocidade, em cima
    }
}
