package com.rocha.jancarlauncher.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingSessionPickerTest {

    @Test
    fun `sem sessoes devolve null`() {
        assertNull(NowPlayingSessionPicker.pick(emptyList()))
    }

    @Test
    fun `uma sessao tocando e escolhida`() {
        val sessions = listOf(SessionInfo("a", isPlaying = true, hasMetadata = true, lastActiveMs = 1000))
        assertEquals("a", NowPlayingSessionPicker.pick(sessions))
    }

    @Test
    fun `duas tocando escolhe a mais recente`() {
        val sessions = listOf(
            SessionInfo("a", isPlaying = true, hasMetadata = true, lastActiveMs = 1000),
            SessionInfo("b", isPlaying = true, hasMetadata = true, lastActiveMs = 2000),
        )
        assertEquals("b", NowPlayingSessionPicker.pick(sessions))
    }

    @Test
    fun `nenhuma tocando mas uma com metadata e escolhida`() {
        val sessions = listOf(
            SessionInfo("a", isPlaying = false, hasMetadata = true, lastActiveMs = 500),
            SessionInfo("b", isPlaying = false, hasMetadata = false, lastActiveMs = 1500),
        )
        assertEquals("a", NowPlayingSessionPicker.pick(sessions))
    }

    @Test
    fun `nenhuma tocando e nenhuma com metadata devolve null`() {
        val sessions = listOf(
            SessionInfo("a", isPlaying = false, hasMetadata = false, lastActiveMs = 500),
        )
        assertNull(NowPlayingSessionPicker.pick(sessions))
    }

    @Test
    fun `prioriza tocando mesmo que outra com metadata seja mais recente`() {
        val sessions = listOf(
            SessionInfo("a", isPlaying = true, hasMetadata = true, lastActiveMs = 500),
            SessionInfo("b", isPlaying = false, hasMetadata = true, lastActiveMs = 9000),
        )
        assertEquals("a", NowPlayingSessionPicker.pick(sessions))
    }
}
