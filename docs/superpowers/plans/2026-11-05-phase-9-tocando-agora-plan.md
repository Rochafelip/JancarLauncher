# Fase 9 — Plano

Spec: `../specs/2026-11-05-phase-9-tocando-agora-design.md`. Pacote `media/`.

## T9.1 `NowPlayingSessionPicker` (teste primeiro)
`media/NowPlayingSessionPicker.kt`: modelo `data class SessionInfo(val id: String, val isPlaying: Boolean, val hasMetadata: Boolean, val lastActiveMs: Long)` (sem tipos do Android, testável em JVM) + `fun pick(sessions: List<SessionInfo>): String?` (devolve o `id` escolhido). `NowPlayingSessionPickerTest`: vazio→null; uma tocando→ela; duas tocando→a de `lastActiveMs` maior; nenhuma tocando mas uma com metadata→essa; nenhuma com metadata→null.

## T9.2 Modelo de UI
`media/NowPlayingUi.kt`: `data class NowPlayingState(val title: String?, val artist: String?, val art: ByteArray?, val isPlaying: Boolean)` + `NowPlayingState.EMPTY`. Mapeamento de `MediaMetadata`/`PlaybackState` reais para esse modelo fica isolado numa função `mapToUiState(metadata, playbackState)`, testável sem `MediaController` real.

## T9.3 `NowPlayingListenerService`
`media/NowPlayingListenerService.kt extends NotificationListenerService`. Registrar no manifesto com a permissão `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` e intent-filter `android.service.notification.NotificationListenerService`. Expor `MediaSessionManager.getActiveSessions(ComponentName(this, NowPlayingListenerService::class.java))` via um objeto acessível (ex. companion com `StateFlow` das sessões ativas, atualizado em `onListenerConnected`/`onNotificationPosted`).

## T9.4 Verificação de permissão
`media/NotificationAccess.kt`: `fun isGranted(context): Boolean` usando `NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)`. Teste de unidade trivial isolando a chamada atrás de uma interface, se necessário para mockar.

## T9.5 Card na Home
`ui/home/NowPlayingCard.kt`: usa T9.1+T9.2 para decidir o que mostrar; `AnimatedVisibility` quando `NowPlayingState == EMPTY`. Sem permissão: estado vazio com botão "Conceder acesso a notificações" → `SystemSettingsOpener`. Posição: entre relógio e chips (`HomeScreen.kt`).

## T9.6 Verificação no emulador
Conceder acesso a notificações manualmente; abrir um player (ex. música de exemplo do AVD); conferir capa/título/artista aparecendo e atualizando ao trocar de faixa; revogar a permissão e confirmar que o card some sem crash.

## T9.7 Commit
`feat: card "Tocando agora" com MediaSessionManager`.
