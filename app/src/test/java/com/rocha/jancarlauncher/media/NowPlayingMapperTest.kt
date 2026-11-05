package com.rocha.jancarlauncher.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingMapperTest {

    @Test
    fun `mapeia titulo e artista`() {
        val state = NowPlayingMapper.map("Faixa", "Artista", null, NowPlayingMapper.STATE_PLAYING, 0L)
        assertEquals("Faixa", state.title)
        assertEquals("Artista", state.artist)
    }

    @Test
    fun `tocando reflete isPlaying`() {
        val playing = NowPlayingMapper.map(null, null, null, NowPlayingMapper.STATE_PLAYING, 0L)
        assertTrue(playing.isPlaying)

        val paused = NowPlayingMapper.map(null, null, null, 2 /* STATE_PAUSED */, 0L)
        assertFalse(paused.isPlaying)
    }

    @Test
    fun `shuffle suportado quando a flag esta no bitmask`() {
        val with = NowPlayingMapper.map(null, null, null, 2, NowPlayingMapper.ACTION_SET_SHUFFLE_MODE)
        assertTrue(with.supportsShuffle)

        val without = NowPlayingMapper.map(null, null, null, 2, 0L)
        assertFalse(without.supportsShuffle)
    }

    @Test
    fun `art nula mapeia para null`() {
        val state = NowPlayingMapper.map(null, null, null, 2, 0L)
        assertEquals(null, state.art)
    }
}
