package com.rocha.jancarlauncher.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicResumeTest {
    private val jancar = LastSource.Jancar

    @Test fun onlyWhenEnabled_withLastSource_andNothingPlaying() {
        assertFalse(MusicResume.shouldResume(false, jancar, null, playingNow = false))
        assertFalse(MusicResume.shouldResume(true, null, null, playingNow = false))
        assertFalse(MusicResume.shouldResume(true, jancar, null, playingNow = true))
        assertTrue(MusicResume.shouldResume(true, jancar, "com.rocha.jancarlauncher", playingNow = false))
    }

    @Test fun projectionInFront_letsThePhoneDecide() {
        assertFalse(MusicResume.shouldResume(true, jancar, "com.google.android.projection.sink", playingNow = false))
        assertFalse(MusicResume.shouldResume(true, jancar, "com.autochips.carplayapp", playingNow = false))
    }

    @Test fun encodeDecode_roundTrip() {
        assertEquals(LastSource.Session("com.spotify.music"), LastSource.decode(LastSource.Session("com.spotify.music").encode()))
        assertEquals(LastSource.Jancar, LastSource.decode(LastSource.Jancar.encode()))
        assertNull(LastSource.decode("lixo"))
        assertNull(LastSource.decode(null))
    }
}
