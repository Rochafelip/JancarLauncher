# JancarLauncher — Plano de implementação

Spec: `docs/superpowers/specs/2026-11-05-jancar-launcher-design.md`. Pré-requisito: spec aprovada.
Fonte única de verdade para pacotes/activities: `app/CLAUDE.md`. Pacote base: `com.rocha.jancarlauncher`.

Cada fase termina com algo instalável e verificável. TDD nas partes lógicas (fases 2, 3, 5).

## Fase 0 — Resolver bloqueios (usuário, em paralelo ao código)
- [ ] `adb shell dumpsys package com.jancar.music` → anotar activity do bloco `android.intent.action.MAIN`.
- [ ] Descobrir o app Vídeo (`dumpsys window | grep mCurrentFocus` com ele aberto, ou `pm list packages | grep -i -E "video|movie"`).
- [ ] Salvar rollback: `adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher`.
- [x] Foto do carro: já extraída para `docs/reference/boot-background.jpg` (1024×600). Layout/medidas: spec §5.1.
- [ ] (Opcional) Conferir `getprop` da central se algum comportamento de API 27 divergir (fingerprints inconsistentes, ver spec §3.1).
Saída: valores para `AppTargets`. **Não bloqueia as fases 1–4.**

## Fase 1 — Base do projeto (Compose + HOME)
1. `libs.versions.toml` + `app/build.gradle.kts`: adicionar Compose (BOM, ui, foundation, material3 mínimo ou só foundation, activity-compose, lifecycle-viewmodel-compose, datastore-preferences). Com AGP 9.x, conferir se o plugin `org.jetbrains.kotlin.plugin.compose` é necessário e adicioná-lo; `buildFeatures { compose = true }`. Manter `minSdk 27`.
2. `AndroidManifest.xml`: `MainActivity` com `MAIN` + `HOME` + `DEFAULT` (+ `LAUNCHER` para facilitar teste), `launchMode=singleTask`, `stateNotNeeded`, `screenOrientation=landscape`, `configChanges` de orientação/tamanho; permissão `RECEIVE_BOOT_COMPLETED`.
3. `MainActivity` com `setContent { Text("ok") }` e remover appcompat/material se não usados (tema Compose próprio; `themes.xml` mínimo sem ActionBar).
**Verificar:** `./gradlew assembleDebug` passa; instala no emulador API 27 1024×600 e abre; aparece como opção de Home.

## Fase 2 — Tema e design tokens
1. `ui/theme/Palette.kt` com as duas paletas + acento `#6fa3d8` + os tokens de borda (spec §5.1); `JancarTheme(dark)`; tamanhos mínimos de toque (48 dp) como constante. Fonte Inter (TTF 200/400/500/600) em `res/font/` + licença OFL em `docs/`/assets.
2. `settings/ThemeStore.kt` (DataStore) + teste unitário de persistência (round-trip).
3. Ícones (inline `ImageVector`/vector drawables): sol/lua, brilho, gaveta, Wi-Fi, BT, GPS, câmera, ajustes, play/pause/anterior/próxima.
**Verificar:** alternar tema na tela de teste e reabrir o app mantém a escolha.

## Fase 3 — Lançamento de apps (núcleo lógico)
1. **Teste primeiro:** `AppLauncherTest` — alvo inexistente não lança exceção; retorna resultado `NotFound`.
2. `launch/AppTargets.kt` (tabela única: Auto-AA, Auto-CarPlay, Rádio, Música, Vídeo, Back-camera, gaveta). Música/Vídeo conforme fase 0, ou fallback do spec.
3. `launch/AppLauncher.kt`: `launch(target): Result` com `try/catch ActivityNotFoundException` e `SecurityException`; `FLAG_ACTIVITY_NEW_TASK`; verificação prévia com `PackageManager` para não cair em `ResolverActivity`.
4. **Teste primeiro:** `ProjectionChooserTest` (iPhone pareado → CarPlay; Android → AA; nenhum → AA; override manda). Depois `launch/ProjectionChooser.kt` + leitura de `BluetoothAdapter.bondedDevices` (permissão `BLUETOOTH` na API 27) e estado USB.
**Verificar:** testes unitários verdes; botão de teste abre o Rádio no aparelho real.

## Fase 4 — Telas Home + Boas-vindas + Gaveta (UI estática com dados reais mínimos)
1. Navegação simples (estado sealed `Screen`, sem lib de navegação).
2. `WelcomeScreen`: copiar `docs/reference/boot-background.jpg` para `res/drawable-nodpi/`; degradê e tipografia conforme spec §5.1; relógio (atualização por minuto), toque → Home; exibida uma vez por boot.
3. `HomeScreen`: relógio/data, ícones topo-dir, chips, 4 blocos ligados ao `AppLauncher`/`ProjectionChooser`.
4. `DrawerScreen`: grid a partir de `PackageManager` + lista curada com tag App/Sistema; ícones com cache; apps ausentes aparecem desabilitados (ou ocultos — **decidir com o usuário ao ver na central**).
5. Painel de brilho (slider). `WRITE_SETTINGS` + fallback de janela, conforme spec §6.5.
Medidas (dp = px do preview) estão no spec §5.1; **não depender de pacotes já desativados na central** (Play Store, Chrome, YouTube, Maps, OTA). Tratar app desativado via `getApplicationEnabledSetting`.
**Verificar:** comparar visualmente com o preview HTML em 1024×600 @160dpi; todos os alvos ≥ 48 dp; troca de tema afeta as 3 telas.

## Fase 5 — REMOVIDA
O card "Tocando agora" e o acesso a notificações foram cortados por decisão do usuário (ver spec §6.3). A numeração das fases 6 e 7 foi mantida.

## Fase 6 — Boot e instalação como HOME
1. `boot/BootReceiver` (`BOOT_COMPLETED`) iniciando `MainActivity`.
2. `docs/INSTALL.md`: `adb install -r`, `set-home-activity com.rocha.jancarlauncher/.MainActivity`, rollback, lembrete de que o ADB Wi-Fi é instável (reconectar com `adb connect <IP_DA_CENTRAL>:5555`).
3. Teste de ciclo: reiniciar central → abre Boas-vindas → Home; botão Home do sistema volta à Home; rollback restaura o launcher de fábrica.
**Verificar:** os três pontos acima no aparelho real.

## Fase 7 — Acabamento
- Ícone do app; strings em pt-BR; revisão de uso de memória (`dumpsys meminfo` na central, meta PSS ≤ ~70 MB e **sem crescer após horas de uso**; só ~350 MB livres no sistema) e de frames; remover logs; resolver pendências não-bloqueantes (pacotes Scrcpy/OBD, `backcarapp`).
- Revisão com `/code-review` antes de considerar pronto.

## Definição de pronto
- [ ] Instala e roda na central real com minSdk 27 sem crash após reinício e após desativar um app-alvo.
- [ ] Os 4 blocos abrem o destino certo sem diálogo de escolha (Música/Vídeo conforme fase 0).
- [ ] Auto/CarPlay escolhido antes do `startActivity`.
- [ ] Tema persiste; brilho funciona (ou fallback documentado).
- [ ] Todos os alvos de toque ≥ 44 dp; paleta com hex exatos.
- [ ] Rollback testado e documentado.

## Riscos
- ADB instável → instalar por USB ou repetir `connect`; testar rollback antes de `set-home-activity`.
- AGP 9.x + Kotlin/Compose: versões podem exigir ajuste (fase 1, passo 1).
- Brilho depende de tela/permissão que a central pode não ter.
