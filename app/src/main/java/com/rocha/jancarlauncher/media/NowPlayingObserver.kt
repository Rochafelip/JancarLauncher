package com.rocha.jancarlauncher.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "JancarNowPlaying"

/** Ações de transporte da sessão escolhida; sem sessão, as chamadas são no-op (seguras). */
class NowPlayingHandle(
    val state: State<NowPlayingState>,
    private val controllerRef: () -> MediaController?,
) {
    private fun safe(action: (MediaController) -> Unit) {
        val c = controllerRef() ?: return
        runCatching { action(c) }.onFailure { Log.w(TAG, "comando de transporte falhou", it) }
    }

    fun play() = safe { it.transportControls.play() }
    fun pause() = safe { it.transportControls.pause() }
    fun next() = safe { it.transportControls.skipToNext() }
    fun previous() = safe { it.transportControls.skipToPrevious() }
    // Sem toggleShuffle(): MediaController.getShuffleMode()/setShuffleMode() são API 28/29+; a central
    // roda API 27 de verdade. O botão de aleatório fica só habilitado/desabilitado (ver spec da Fase 10).
}

private fun toSessionInfo(controller: MediaController): SessionInfo {
    val playbackState = controller.playbackState
    val metadata = controller.metadata
    return SessionInfo(
        id = controller.sessionToken.toString(),
        isPlaying = playbackState?.state == NowPlayingMapper.STATE_PLAYING,
        hasMetadata = metadata != null,
        lastActiveMs = playbackState?.lastPositionUpdateTime ?: 0L,
    )
}

private fun toUiState(controller: MediaController): NowPlayingState {
    val metadata = controller.metadata
    val playbackState = controller.playbackState
    val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val artBytes = art?.let {
        val stream = java.io.ByteArrayOutputStream()
        it.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
    return NowPlayingMapper.map(
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
        art = artBytes,
        playbackState = playbackState?.state ?: PlaybackState.STATE_NONE,
        actions = playbackState?.actions ?: 0L,
    )
}

/** Observa as sessões ativas (via [NowPlayingListenerService]) e expõe o estado de UI + ações de transporte. */
@Composable
fun rememberNowPlaying(): NowPlayingHandle {
    val context: Context = LocalContext.current
    val state = remember { mutableStateOf(NowPlayingState.EMPTY) }
    val currentController = remember { arrayOfNulls<MediaController?>(1) }
    val scope = rememberCoroutineScope()

    DisposableEffect(context) {
        if (!NotificationAccess.isGranted(context)) {
            state.value = NowPlayingState.EMPTY
            return@DisposableEffect onDispose {}
        }

        var registeredController: MediaController? = null
        lateinit var callback: MediaController.Callback

        fun reselect() {
            val controllers = NowPlayingListenerService.activeSessions.value
            registeredController?.let { runCatching { it.unregisterCallback(callback) } }

            val infos = controllers.map(::toSessionInfo)
            val pickedId = NowPlayingSessionPicker.pick(infos)
            val picked = controllers.firstOrNull { it.sessionToken.toString() == pickedId }

            if (picked == null) {
                registeredController = null
                currentController[0] = null
                state.value = NowPlayingState.EMPTY
                return
            }
            picked.registerCallback(callback)
            registeredController = picked
            currentController[0] = picked
            state.value = toUiState(picked)
        }

        callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                registeredController?.let { state.value = toUiState(it) }
            }
            override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                registeredController?.let { state.value = toUiState(it) }
            }
            override fun onSessionDestroyed() { reselect() }
        }

        reselect()

        val job = scope.launch(Dispatchers.Main) {
            NowPlayingListenerService.activeSessions.collect { reselect() }
        }

        onDispose {
            registeredController?.let { runCatching { it.unregisterCallback(callback) } }
            job.cancel()
        }
    }

    return NowPlayingHandle(state) { currentController[0] }
}
