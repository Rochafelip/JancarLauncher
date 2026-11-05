# Fase 10 — Plano

Spec: `../specs/2026-11-05-phase-10-controles-midia-design.md`. Depende da Fase 9 (`media/`).

## T10.1 Ações disponíveis (teste primeiro)
`media/TransportActions.kt`: `fun supportsShuffle(actions: Long): Boolean = actions and PlaybackState.ACTION_SET_SHUFFLE_MODE != 0L`. `TransportActionsTest`: bitmask com e sem a flag.

## T10.2 Envio de comandos
`media/TransportCommander.kt`: recebe o `MediaController` da sessão escolhida (T9.1/T9.3) e expõe `fun play()/pause()/next()/previous()`, cada um envolto em `try { } catch (e: RuntimeException) { Log.w(...) }`. Sem teste de unidade direto (API Android não mocka bem); cobrir via T10.1 (decisão de habilitar/desabilitar) que é a parte determinística.

## T10.3 Ligação na UI
`NowPlayingCard.kt` (Fase 9) ganha os 4 botões reais: aleatório (desabilitado se `!supportsShuffle`), anterior, play-pause (ícone conforme `PlaybackState.state`), próxima — todos usando `Pressable` existente (feedback de escala 0,98).

## T10.4 Verificação no emulador
Com um player tocando: tocar play/pause/anterior/próxima e confirmar que o player real muda de estado; pausar pelo próprio app de música (fora do launcher) e confirmar que o ícone no launcher atualiza sozinho.

## T10.5 Commit
`feat: controles de mídia funcionais no card Tocando agora`.
