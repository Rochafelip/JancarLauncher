# Ledger de execução — JancarLauncher

Projeto não é repositório git: sem worktree/commits; este arquivo é o registro.

## Fase 1 — Base Compose + HOME
Task 1.1–1.6: complete. Evidência: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (2026-11-05), APK `app/build/outputs/apk/debug/app-debug.apk` (~9,9 MB). Manifesto mesclado confere: minSdk 27, HOME, singleTask, landscape, RECEIVE_BOOT_COMPLETED.
Task 1.7: **parcial.** Não há AVD API 27 (só imagens 35/37) nem aparelho conectado; verificação de execução (sem faixa branca, resolve-activity HOME, sem recriar ao girar) fica pendente para quando houver emulador/aparelho.

Rulings:
- `compileSdk` 36.1 → **37**: as libs atuais (core-ktx 1.19.0, compose-ui 1.12.1) exigem 37; plataforma 37.0 já instalada. `targetSdk` 36 e `minSdk` 27 inalterados (compatibilidade com a central não muda). Custo se errado: nenhum em runtime.
- Versões fixadas: Compose BOM 2026.09.00, activity-compose 1.13.0, lifecycle 2.11.0, datastore 1.3.0, Kotlin/Compose plugin 2.4.20 (o mesmo que o AGP 9.2.1 já usa no cache), coroutines-test 1.11.0. Só estáveis.
- Plugin `org.jetbrains.kotlin.android` **não** aplicado (AGP 9.x traz Kotlin embutido); só o plugin do compilador Compose.
- Removidos appcompat/material, `values-night/themes.xml` e os testes de exemplo (sem testes reais ainda; chegam na Fase 2/3).
- Tema da janela baseado em `android:Theme.Material.NoActionBar` (plataforma) com fundo `#0A0B0F`.

## Fase 2 — Tema, tokens, tipografia, ícones, componentes
Task 2.1 (Palette) e 2.4 (SettingsStore): TDD. RED: `testDebugUnitTest` falhou por `Unresolved reference 'SettingsStore'` (classes inexistentes). GREEN: 10/10 testes (PaletteTest 4, SettingsStoreTest 6), 0 falhas.
Task 2.2/2.3/2.5/2.6/2.7 (Type, Theme, Icons, Components, GalleryActivity debug) + MainActivity ligada ao tema: complete. `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
Verificação visual no emulador `jancar_1024x600` (API 35, 1024×600 @160dpi, criado só para teste): galeria renderiza Inter, ícones, chips, tiles, nos 2 temas (captura conferida). Botão de tema mede 48×48 dp (uiautomator). Cores: fundo escuro lido = (10,11,15) = #0A0B0F; claro = (241,242,244) = #F1F2F4. Persistência: alternar → force-stop → reabrir mantém o claro (`settings.preferences_pb` criado).
Pendente (não verificado): capturas das linhas de baixo da galeria (tiles da gaveta, estado desabilitado) — não rolei a tela; foco por D-pad.

Rulings:
- Fonte Inter: **um TTF variável** (`res/font/inter_variable.ttf`, 877 KB) com `FontVariation.weight`, em vez de 4 TTFs estáticos — um arquivo só, API ≥ 26 ok (minSdk 27). Custo se errado: variável pode renderizar diferente em ROMs antigas → conferir na central.
- Ícones play/pause/anterior/próxima **não criados** (card "Tocando agora" foi removido).
- `Pressable` sem ripple, com escala 0,98 e borda de acento no foco (spec §5.1).
- Tema ainda carrega de forma assíncrona: com tema claro salvo, aparece o fundo escuro até o DataStore responder (no emulador frio levou segundos). Revisar na Fase 7 (medir na central; opção: guardar o tema também em SharedPreferences síncrono).
- AVD de teste `jancar_1024x600` criado em `~/.android/avd` (removível). Os AVDs antigos Pixel_6_* não tinham `.ini`, não iniciam pela CLI.

## Fase 3 — Lançamento de apps (núcleo lógico)
Task 3.1–3.5: TDD. RED: 86 erros `Unresolved reference` (AppLauncher, PackageGateway, AppTargets, ProjectionChooser, LaunchController, LaunchMessages inexistentes). GREEN: `./gradlew testDebugUnitTest` → 42/42 (AppLauncher 9, AppTargets 6, LaunchController 3, LaunchMessages 5, ProjectionChooser 9, SettingsStore 6, Palette 4), 0 falhas; `assembleDebug` OK.
Task 3.6 (verificação no emulador API 35, 1024×600): botões de teste na galeria debug. Rádio → toast "Rádio não está instalado"; Música → "Música não configurado"; Auto → "Auto não está instalado"; nenhum crash (logcat sem FATAL EXCEPTION). Caso positivo: alvo `com.android.settings` aberto pelo AppLauncher → `topResumedActivity=com.android.settings/.Settings`. `startActivity` aparece só em `ContextActivityStarter` (Environment.kt).
NÃO verificado (precisa da central): abrir Rádio/Android Auto/CarPlay reais; leitura de Bluetooth/USB com celular de verdade; ausência de ResolverActivity com `com.autochips.backcarapp` (lançado só por pacote).

Rulings:
- Modelo `AppTarget` usa `pkg: String?` + `activityClass: String?` (não `ComponentName`): tipos do Android quebram em testes JVM ("not mocked"). Custo se errado: nenhum.
- Interfaces `ActivityStarter.startComponent/startLaunchIntentFor` no lugar de `PackageGateway.launchIntentFor(): Intent` do plano — mesma razão (Intent não é testável na JVM).
- `LaunchResult` ganhou `NotConfigured` (Música/Vídeo nulos até a Fase 0), conforme decisão do usuário "activity fixa na tabela".
- `catch (e: RuntimeException)` único cobre ActivityNotFound/Security/demais (subclasses); Errors não são capturados de propósito.
- `ProjectionChooser`: `usbPhoneAttached` está na entrada mas não altera o resultado (Android Auto já é o padrão); heurística Apple por nome (iphone/ipad/apple). Afinar na central.
- Sem `<queries>`/QUERY_ALL_PACKAGES: na central (API 27) não há filtro de visibilidade de pacotes. No emulador API 35 isso apareceria como "não instalado" para apps existentes, exceto o teste de Ajustes que é visível por ser app de sistema. Revisar na Fase 4 (gaveta) se for testar em dispositivo moderno.
- `LaunchController.open` aceita qualquer AppTarget; a lista da gaveta (12 apps) fica para a Fase 4.

## Fase 4 — Telas: Boas-vindas, Home, Gaveta
Task 4.1–4.9: complete. TDD nas partes puras. RED (69 erros `Unresolved reference`: WelcomePolicy, ClockFormatter, BrightnessMapper, SystemSettingsOpener, DrawerCatalog) → GREEN. Depois, 2 mudanças de comportamento também em RED→GREEN: brilho só do usuário (`userBrightness`, `BrightnessMapper.fromSystem`) e plural da Gaveta (`installedLabel`). Suite final: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 76/76 testes, 0 falhas.
Verificação no emulador `jancar_1024x600` (API 35, instalação limpa): Boas-vindas na 1ª abertura (hora real 20:51, data pt-BR) → toque → Home; Home e Gaveta conferidas com capturas nos 2 temas contra o preview; painel de brilho em 260 dp no canto (top 146 / right 64), arrasto 40%→88%, persistiu após force-stop e reabrir; reabrir na mesma sessão vai direto à Home (sem Boas-vindas); botão voltar na Gaveta → Home; intent HOME com Gaveta aberta → Home; extra `show_welcome` com app aberto → Boas-vindas e toque → Home; chip Wi-Fi abre `com.android.settings`; Música → toast "Música não configurado"; tile Scrcpy desabilitado → toast "Scrcpy não configurado"; Gaveta "1 app instalado" (só Bluetooth é atalho de sistema); sem `FATAL EXCEPTION` no logcat.
NÃO verificado (precisa da central/horas): gatilho real de "acordou" (receiver de SCREEN_ON após ≥10 min — só a política foi testada, e o caminho `show_welcome` por intent); brilho de sistema (WRITE_SETTINGS) — testei só o caminho de janela (appops negado); abrir Rádio/Auto/Câmera de ré reais; D-pad na ordem visual; medidas ±2 dp contra o preview além da inspeção visual; memória/uso de CPU.

Rulings:
- `WelcomePolicy` ficou em duas funções (`shouldShowOnLaunch`, `shouldShowOnWake`), não numa só como no plano — mais simples de testar. `screenOffAt` fica só em memória (se o processo morrer, vale só a regra de reinício).
- Brilho: o launcher **não aplica brilho na abertura** a menos que o usuário já tenha escolhido um (`userBrightness` null por padrão); slider começa no brilho atual do sistema. Substituiu `brightness` (padrão 70) do plano para não sobrescrever o brilho da central sem pedido. Custo se errado: nenhum.
- Brilho: a janela do launcher sempre muda; o `SCREEN_BRIGHTNESS` do sistema só se `Settings.System.canWrite`. Conceder pela ADB: `adb shell appops set com.rocha.jancarlauncher WRITE_SETTINGS allow` (vai para o INSTALL.md na Fase 6).
- Gaveta: só Bluetooth (atalho de Settings) e os 4 pacotes conhecidos (VLC `org.videolan.vlc`, NewPipe `org.schabi.newpipe`, F-Droid `org.fdroid.fdroid`, Lightning `acr.browser.lightning`) estão configurados; **Galeria, Arquivos, Música BT, EQ, Veículo, Scrcpy e OBD-II ficam "não configurado"** até a Fase 0 listar os pacotes (`pm list packages`). Não chutei pacotes.
- Tiles desabilitados continuam clicáveis (toast explicando), com 40% de opacidade.
- Slider próprio (toque, arrasto e D-pad ±5) sem Material; persiste só ao soltar (poupa escrita no eMMC).
- Botão voltar: fecha o painel de brilho, depois Gaveta→Home; na Home não faz nada.
- Boas-vindas/Home/Gaveta usam `UiActions` (toast em pt-BR); nenhum `startActivity` novo fora de `ContextActivityStarter`/`ContextSettingsStarter`.

## Fase 6 — Boot e instalação como HOME
Task 6.1–6.7: complete. TDD: RED (11 erros `Unresolved reference`: RestartPolicy, BootReceiver) → GREEN. Suite final: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 83/83 testes, 0 falhas (RestartPolicy 5, BootReceiver 2).
Ensaio no emulador `jancar_1024x600` (API 35), tudo com `adb`: `set-home-activity` do nosso app + botão Home abre o launcher; rollback (NexusLauncher) + botão Home volta ao original; `force-stop` + Home reabre; `am crash` ×5 → 3 reinícios "autorizados" e o 4º e 5º "NEGADO (limite de 3 por minuto)" no logcat, histórico `1793912415282,…419712,…424230` gravado em `shared_prefs/crash_restarts.xml` e mantido entre processos; reboot real (`adb reboot`): o sistema iniciou `com.rocha.jancarlauncher/.MainActivity` como HOME (uid 0) 4 s após `boot_completed`, `boot_count` 6→7 e a Boas-vindas apareceu (captura conferida); logs mostram o `BootReceiver` disparando no boot (START a partir do uid do app com `BAL_ALLOW_ALLOWLISTED_COMPONENT`). HOME do emulador restaurado para o NexusLauncher ao final.
NÃO verificado (precisa da central, API 27): tudo acima na central real; tempo de boot até a Home; botão Home físico; `appops set ... WRITE_SETTINGS allow` (só testei `deny` no emulador); T6.4 (botão Home volta à Home) já cobertura da Fase 4 (intent HOME com Gaveta aberta → Home). `BootReceiver` sem teste unitário de `onReceive` (só `isBootAction`); o disparo real foi visto no logcat do reboot.

Findings:
- **A definição de HOME precisa de ~25 s para ser gravada em disco.** `adb reboot` imediatamente depois de `set-home-activity` perdeu a preferência (apareceu o seletor "Usar JancarLauncher como Home"); esperando 25 s ela sobreviveu a 2 reboots. Documentado em `docs/INSTALL.md` §4. Na central pode variar.
- Durante o 1º reboot bem-sucedido apareceram dois inícios de Ajustes (GPS e Wi-Fi) vindos do uid do nosso app, 7 e 12 s depois do boot — equivalem a toques nos chips, que não vieram dos meus comandos (provável interação com a janela do emulador na tela do usuário). Não reproduzido no 3º reboot (sem interferência). Não é defeito conhecido do app.
- Shell não consegue enviar `BOOT_COMPLETED` (broadcast protegido) nem matar o processo do app (`kill` → "Operation not permitted"); por isso usei reboot real e `am crash`.

Rulings:
- O histórico de reinícios fica em SharedPreferences com `commit()` (síncrono) e **não** só em memória como o plano (T6.1) sugeria: a memória morre com o processo que crasha e o limite nunca seria atingido (laço infinito). Custo se errado: nenhum.
- Registros de reinício "no futuro" (RTC da central mudou) são descartados em vez de bloquear para sempre.
- Reinício automático após crash só quando o crash passa pelo handler (exceção Kotlin/Java). Crash nativo ou OOM-kill não passam por ele; nesses casos quem reabre a HOME é o próprio sistema.
- Adicionei logs `JancarCrash` de "autorizado/NEGADO" (não estavam no plano) — necessários para provar o limite e úteis na central.
- Botão "Forçar crash" só no build debug (GalleryActivity); não existe no release.
- `T6.4` (botão Home volta à Home) não foi refeito: já implementado e verificado na Fase 4.

## Fase 7 — Acabamento
Task 7.1–7.8: complete (com ressalvas abaixo). Suite final: `./gradlew lintDebug assembleDebug assembleRelease testDebugUnitTest` → BUILD SUCCESSFUL, **96/96 testes**, lint 0 erros / 6 avisos.
Feito: ícone adaptativo novo (anel prata + ponto azul-aço sobre `#0A0B0F`; webp antigos removidos); release com R8 + shrink de recursos, assinado com a chave de debug (1,5 MB vs 10,8 MB do debug); lint corrigido (3 erros `MissingPermission` do Bluetooth, `commit()` via KTX); `README.md`, `app/CLAUDE.md` atualizado, `docs/superpowers/DONE.md` com evidências.
Medições no emulador (release): partida a frio 1,7 / 2,0 / 0,7 s (emulador x86 com renderização por software — **não representa a central**; meta do spec < 1,5 s não confirmada). PSS 24–33 MB; 2 rodadas de 31 amostras; 1ª rodada interrompida pelo fechamento da janela do emulador no ciclo 30 (ciclos 31–45 vazios, descartados). Sobe ~10 ciclos de aquecimento e depois 8–14 KB/ciclo.
Contraste (WCAG, calculado): texto `muted` 4,65–5,89:1 ok; `faint` sobre bg/surface **2,4–2,7:1** (legenda "MULTIMÍDIA" e tag APP/SISTEMA) abaixo de 4,5:1; acento `#6fa3d8` no tema claro **2,37:1** (thumb do slider e borda de foco). **Hex mantidos** — a paleta é decisão do usuário; só registrado.
Revisão final (`/code-review medium app/src`, sem git → arquivos inteiros): 9 achados. Reclassifiquei por efeito.
- Corrigidos (cada um RED→GREEN, suíte 96/96):
  - `Final: fixed` D-pad/volante no slider de brilho não persistia (voltava ao valor antigo ao reabrir) — `SliderKeysTest` (5) RED→GREEN; verificado no emulador: 86%→100%, mantido após reabrir.
  - `Final: fixed` Boas-vindas perdida se o processo do launcher morrer durante a suspensão (`screenOffAt` só em memória) — `WakeWelcomeTrackerTest` (8) RED→GREEN; registro em SharedPreferences com `elapsedRealtime`, decisão também na abertura do app (`onLaunch`). Verificado no emulador: ~10,7 min apagada → Boas-vindas ao acordar; 25 s → não aparece.
- Adiados (minor), nenhum afeta a central API 27:
  - `Final: minor (deferred):` sem `<queries>`/`QUERY_ALL_PACKAGES` — em Android 11+ (não na central) todo app-alvo apareceria "não instalado".
  - `Final: minor (deferred):` Boas-vindas só é marcada como vista ao tocar; se o botão Home a substituir e o processo morrer, reaparece uma vez.
  - `Final: minor (deferred):` `BOOT_COUNT` indisponível (-1) → Boas-vindas nunca mais após reboot nesse ROM (o gatilho de acordar continua).
  - `Final: minor (deferred):` `events` (SharedFlow sem replay) pode perder um intent que chegue nos milissegundos da criação da Activity.
  - `Final: minor (deferred):` heurística Apple por substring ("Pineapple Pixel" → CarPlay); sem como escolher manualmente (o `userOverride` existe mas não há controle na UI).
  - `Final: minor (deferred):` toast de "Auto" usa rótulo genérico, não diz se faltou Android Auto ou CarPlay.
  - `Final: minor (deferred):` `allowBackup=true` pode restaurar `last_boot_count`/brilho de outro aparelho (a central não tem backup do Google).
- Observação minha (não está no review): `onLaunch` apaga o registro de tela apagada mesmo quando ainda abaixo de 10 min; um reinício por crash durante a suspensão faz a Boas-vindas do próximo acordar não aparecer. Raro; adiado.

Rulings:
- Strings **não** migradas para `strings.xml`: o app é monolíngue (pt-BR) e a lógica testada em JVM (`LaunchMessages`, `ClockFormatter`, `DrawerCatalog`) não tem `Context`. Custo se errado: tradução futura exige refatorar.
- LeakCanary **não** adicionado: o APK de debug é o que vai para a central e ele consumiria memória lá; usei contagem de Activities/AppContexts no `meminfo` (sempre 1 Activity).
- Baseline Profile **não** gerado (ARMv7 da central não é suportado pelo gerador; ganho incerto).
- Release assinado com a chave de debug (`signingConfig = debug`): a central não valida assinatura; evita um keystore que o usuário teria de guardar.
- Versões novas de AGP (9.5.1), Kotlin (2.4.21) e core-ktx (1.19.1) **não** atualizadas: fora do escopo e sem benefício para a central.
- Orientação fixa em landscape mantida (aviso do Android 16 não afeta a API 27).
- Chip "Câmera de ré" mantido: abre `com.autochips.backcarapp` pelo pacote; se não aceitar abertura manual, vai aparecer toast/falha silenciosa — decisão final depende de testar na central.
- Não foi feito o soak de 2 h do plano (T7.1): duas rodadas de ~20 min no emulador. Reportado como não confirmado.
- A janela do emulador foi fechada sem querer durante a 1ª rodada; refiz a rodada com o APK final. Possível causa dos "toques fantasma" de GPS/Wi-Fi vistos na Fase 6 (interação com a janela).
