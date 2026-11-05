# Fase 0 — Resolver bloqueios (spec)

Mestre: `2026-11-05-jancar-launcher-design.md` §10. Executor: **o usuário**, na central, via ADB. Não bloqueia as fases 1–4.

## Objetivo
Descobrir os dois destinos que faltam (Música e Vídeo) e deixar o rollback salvo antes de qualquer teste de HOME.

## Entregáveis
| # | Entregável | Onde fica |
|---|---|---|
| 1 | Activity exata de `com.jancar.music` | `AppTargets.MUSIC` (fase 3) |
| 2 | Pacote + activity do app Vídeo | `AppTargets.VIDEO` (fase 3) |
| 3 | Comando de rollback confirmado | `docs/INSTALL.md` (fase 6) |
| 4 | Pacotes de Scrcpy e OBD-II (não bloqueante) | lista da gaveta (fase 4) |
| 5 | `com.autochips.backcarapp` abre manualmente? (não bloqueante) | decisão do chip "Câmera de ré" |

## Critérios de aceite
- Cada entregável 1–2 vem como `pacote/activity` testado com `adb shell am start -n <pkg>/<activity>` abrindo o app certo **sem diálogo de escolha**.
- Resultados anotados no fim deste arquivo (seção "Resultados").

## Não-objetivos
Nenhum código; nenhuma alteração permanente na central (só leitura + `am start`).

## Resultados (preencher)
- Música: `____`
- Vídeo: `____`
- Scrcpy: `____` · OBD-II: `____`
- backcarapp manual: `____`
