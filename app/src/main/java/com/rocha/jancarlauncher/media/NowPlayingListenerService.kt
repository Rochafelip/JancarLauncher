package com.rocha.jancarlauncher.media

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Serviço "escutador de notificações" — a única forma de ler sessões de mídia de outros apps.
 * O usuário concede uma vez em Configurações → Acesso a notificações (ver [NotificationAccess]).
 * Não lê nem usa o conteúdo das notificações em si, só habilita [MediaSessionManager.getActiveSessions].
 */
class NowPlayingListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _activeSessions.value = emptyList()
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) = refresh()

    private fun refresh() {
        runCatching {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(this, NowPlayingListenerService::class.java)
            _activeSessions.value = manager.getActiveSessions(component)
        }.onFailure { Log.w(TAG, "falha ao ler sessões de mídia ativas", it) }
    }

    companion object {
        private const val TAG = "JancarNowPlaying"
        private val _activeSessions = MutableStateFlow<List<android.media.session.MediaController>>(emptyList())
        /** Sessões de mídia ativas no sistema agora; vazio se o serviço não estiver conectado. */
        val activeSessions: StateFlow<List<android.media.session.MediaController>> = _activeSessions.asStateFlow()
    }
}
