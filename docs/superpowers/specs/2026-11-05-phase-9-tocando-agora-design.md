# Fase 9 — Card "Tocando agora" (spec)

Supersede a decisão da Fase 5 ("REMOVIDA"), revertida pelo usuário em 2026-11-05. Reintroduz o card com dados reais via `MediaSessionManager`.

## Objetivo
Mostrar capa, título e artista da faixa tocando no momento, lidos da sessão de mídia ativa do sistema.

## Requisitos
1. **Permissão**: `BIND_NOTIFICATION_LISTENER_SERVICE` (acesso a notificações), concedida uma vez pelo usuário em Configurações. Verificar com `NotificationManagerCompat.getEnabledListenerPackages`. Sem a permissão, o card não aparece (sem pedir em loop).
2. **`NowPlayingSessionPicker`** (`media/`), lógica pura/testável: dado uma lista de `(token, metadata, playbackState, lastActiveTime)`, escolhe a sessão "ativa" preferindo a que está tocando (`STATE_PLAYING`) mais recente; sem nenhuma tocando, usa a última sessão com metadata válida; sem nenhuma, retorna null (card some).
3. **Serviço**: `NowPlayingListenerService : NotificationListenerService`, registrado no manifesto, expõe a lista de sessões via `MediaSessionManager.getActiveSessions(componentName)` (requer o serviço estar ativo, não basta a permissão de notificação).
4. **UI**: card entre o relógio e a fileira de chips, como no preview visual. Capa (`MediaMetadata.METADATA_KEY_ALBUM_ART`/`ART`), título, artista. Sem capa: ícone de nota musical (já existe em `ui/icons/Icons.kt`). Sem sessão: card não ocupa espaço (`AnimatedVisibility`).
5. **Atualização em tempo real**: `MediaController.Callback.onMetadataChanged`/`onPlaybackStateChanged`, propagado para a UI via `StateFlow`.

## Decisões minhas
- Preferir sessão tocando > última com metadata, nunca a mais antiga sem tocar.
- Sem permissão concedida: nenhum prompt automático repetido; um botão discreto no card vazio ("Conceder acesso a notificações") que abre a tela de Configurações via `SystemSettingsOpener`, só se o usuário tocar.
- Pacote do serviço: `com.rocha.jancarlauncher.media.NowPlayingListenerService`.

## Fora de escopo
Controles de transporte funcionais (Fase 10); barra de progresso/seek.

## Aceite
1. `NowPlayingSessionPickerTest`: seleção com 0/1/várias sessões, com e sem uma tocando.
2. No emulador, com um player de música tocando: capa/título/artista aparecem e atualizam ao trocar de faixa.
3. Sem permissão concedida: card não aparece (ou mostra o botão de conceder acesso, sem crash).
4. Revogar a permissão em tempo de execução: card some sem crash.
