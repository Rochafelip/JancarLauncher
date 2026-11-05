# Critérios de pronto — JancarLauncher

Estado em 2026-11-05. **Tudo foi verificado no emulador (Android 15, API 35, 1024×600 @160 dpi); nada na central real (API 27).** A coluna "Evidência" diz exatamente o que foi visto. Itens marcados ⚠ dependem da central.

| # | Critério | Estado | Evidência |
|---|---|---|---|
| 1 | Roda na central sem crash após reinício e após desativar um app-alvo | ⚠ parcial | Emulador: reboot real (`adb reboot`) abre o launcher como HOME 4 s após o boot, sem crash; alvo ausente/desativado só mostra toast (nenhum `FATAL EXCEPTION`). Central: não testado. |
| 2 | Os 4 blocos abrem o destino certo sem diálogo de escolha | ⚠ não verificado | Intents explícitos (nunca `ResolverActivity`), testados em unidade (`AppLauncherTest`). Rádio, Android Auto e CarPlay reais não abertos (não existem no emulador). **Música e Vídeo seguem "não configurado"** até o usuário informar as activities. |
| 3 | Auto/CarPlay decidido **antes** do `startActivity` | ✅ unidade | `ProjectionChooserTest` (9) e `LaunchControllerTest`: com iPhone pareado só o CarPlay é iniciado, o Android Auto nunca. Leitura real de Bluetooth/USB com celular: ⚠ não testada. |
| 4 | Tema persiste | ✅ emulador | Alternar → `force-stop` → reabrir mantém o tema (Fase 2). |
| 4b | Brilho funciona (ou fallback documentado) | ◐ | Janela: slider 40%→88%, persiste após reabrir; teclas D-pad ajustam e **persistem** (86%→100%, mantido após reabrir). Brilho do **sistema** (`WRITE_SETTINGS`) não verificado; fallback de janela documentado em `INSTALL.md` §5. O launcher não mexe no brilho antes de o usuário escolher. |
| 5 | Alvos de toque ≥ 44 dp; hex exatos | ✅ | Botão de tema/brilho 48×48 dp e chips 48 dp de altura (uiautomator); `PaletteTest` confere cada hex do spec. |
| 6 | Rollback testado e documentado | ✅ emulador / ⚠ central | Emulador: `set-home-activity` do nosso app + Home abre o launcher; rollback + Home volta ao NexusLauncher. Central: o guia (`docs/INSTALL.md`) manda testar antes. |
| 7 | Memória dentro da meta e estável | ◐ | Release (R8) no emulador: PSS 24–33 MB (meta ≤ 70 MB), Java heap ~6,8 MB, 1 Activity sempre. Sobe nos ~10 primeiros ciclos (aquecimento) e na 2ª metade fica em 8–14 KB/ciclo. Rodadas: 31 + 31 amostras (~20 min cada). **Não foi feito o teste de 2 h** e a central é ARMv7 com outra memória — meta não confirmada lá. |
| 8 | Boas-vindas ao ligar o carro (suspensão) | ✅ emulador | Tela apagada ~10,7 min → ao acordar aparece a Boas-vindas; apagada 25 s → não aparece. Boas-vindas também após reboot (`boot_count` 6→7). Caso do processo morto na suspensão coberto por unidade (`WakeWelcomeTrackerTest`), não reproduzido no aparelho. A central pode dormir sem os eventos de tela (MCU/ACC): ⚠. |

## Qualidade

- `./gradlew testDebugUnitTest` → **96/96**, 0 falhas.
- `./gradlew lintDebug` → **0 erros**, 6 avisos (versões novas de AGP/Kotlin/core-ktx, `targetSdk`, e orientação fixa — intencionais para a central).
- Release minificado (R8 + shrink): **1,5 MB** (debug 10,8 MB), assinado com a chave de debug (a central não valida assinatura).
- Revisão de código (`/code-review`, 9 achados): 2 corrigidos com teste (D-pad não salvava brilho; Boas-vindas perdida se o processo morrer na suspensão), 7 adiados — ver `ledger.md`.

## Antes de dar como entregue na central

1. Rodar `adb shell pm list packages` e preencher Música, Vídeo e os 7 apps da Gaveta em `AppTargets.kt` / `DrawerCatalog.kt`.
2. Repetir a lista de `docs/INSTALL.md` §9 na central e anotar o resultado.
3. Conferir se desligar/ligar o carro dispara tela apagada/acesa (Boas-vindas ao acordar) e medir memória lá (`dumpsys meminfo com.rocha.jancarlauncher`).
