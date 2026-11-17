package com.rocha.jancarlauncher.media

import com.rocha.jancarlauncher.launch.AppTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSourceTest {
    @Test fun jancarMusic_radio_andBluetooth_openTheirOwnApp() {
        assertEquals(AppTargets.MUSIC, PlayerSource.target(NowPlayingSource.Jancar, 0, null))
        assertEquals(AppTargets.RADIO, PlayerSource.target(NowPlayingSource.Jancar, 1, null))
        assertEquals(AppTargets.BT_MUSIC, PlayerSource.target(NowPlayingSource.Jancar, 7, null))
    }

    @Test fun otherJancarTypes_openNothing() {
        // Terceiros espelhados (20), CarPlay (25), vídeo (2): abrir a Música trocaria a fonte de áudio.
        assertNull(PlayerSource.target(NowPlayingSource.Jancar, 20, null))
        assertNull(PlayerSource.target(NowPlayingSource.Jancar, 25, null))
        assertNull(PlayerSource.target(NowPlayingSource.Jancar, 2, null))
        assertNull(PlayerSource.target(NowPlayingSource.Jancar, null, null))
    }

    @Test fun session_opensItsPackage_orNothing() {
        assertEquals("com.spotify.music", PlayerSource.target(NowPlayingSource.Session, null, "com.spotify.music")?.pkg)
        assertNull(PlayerSource.target(NowPlayingSource.Session, null, null))
    }
}
