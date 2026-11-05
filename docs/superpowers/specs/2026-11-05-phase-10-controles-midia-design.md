# Fase 10 — Controles de mídia funcionais (spec)

Depende da Fase 9 (precisa da sessão ativa identificada).

## Objetivo
Os botões de transporte do card "Tocando agora" (aleatório / anterior / play-pause / próxima) enviam comandos reais via `MediaController.TransportControls`.

## Requisitos
1. Botões chamam `transportControls.play()/pause()/skipToNext()/skipToPrevious()` na sessão escolhida pelo `NowPlayingSessionPicker`.
2. Ícone de play/pause reflete `PlaybackState.state` real (`STATE_PLAYING` ↔ outros), atualizado pelo mesmo `MediaController.Callback` da Fase 9.
3. Botão "aleatório": se `PlaybackState.actions` não incluir `ACTION_SET_SHUFFLE_MODE`, o botão fica desabilitado (visível, sem ação) — nem toda sessão suporta.
4. Falha ao enviar comando não trava o launcher (`catch (e: RuntimeException)`, mesmo padrão de `LaunchController`).
5. Feedback de toque imediato (escala 0,98 do `Pressable` existente), mesmo que o app de música demore a reagir.

## Decisões minhas
- Sem retry automático se o comando falhar — o usuário toca de novo.

## Fora de escopo
Barra de progresso/seek.

## Aceite
1. Testes de unidade para a lógica de "quais ações estão disponíveis" dado um `PlaybackState.actions` (sem `MediaController` real, que não mocka bem em JVM).
2. No emulador, com um player tocando: play/pause/anterior/próxima mudam o estado real do player.
3. Ícone de play/pause muda sozinho quando a faixa termina ou é pausada pelo próprio app de música (fora do launcher).
