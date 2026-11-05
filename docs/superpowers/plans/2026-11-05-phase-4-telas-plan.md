# Fase 4 — Plano

Spec: `../specs/2026-11-05-phase-4-telas-design.md`. Pacotes `ui/welcome`, `ui/home`, `ui/drawer`, `ui/nav`, `settings`.

## T4.1 Navegação e raiz
`ui/nav/Screen.kt` (`enum class Screen { Welcome, Home, Drawer }`) e `ui/AppRoot.kt`: `var screen by rememberSaveable { mutableStateOf(initial) }`; `when (screen)`; `BackHandler(enabled = screen == Screen.Drawer) { screen = Screen.Home }`. `initial` vem de T4.2.

## T4.2 Regra “mostrar Boas-vindas” (teste primeiro)
- `WelcomePolicyTest`: `shouldShow(currentBootCount, savedBootCount)` → `current != saved`; `saved == null` → true.
- `settings/WelcomePolicy.kt` + `SettingsStore.lastBootCount: Flow<Int?>` / `setLastBootCount`. Leitura: `Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)`. Marcar como visto ao sair da Boas-vindas.
- **Suspensão (confirmado pelo usuário):** `WelcomePolicy.shouldShow(bootCountNow, bootCountSaved, screenOffAtMs?, nowMs, thresholdMs = 10 * 60_000)` → true se o boot mudou **ou** `now - screenOffAt ≥ threshold`. Testes: boot igual + off 5 min → false; boot igual + off 11 min → true; boot diferente → true; `screenOffAt` nulo → só pelo boot.
- `power/ScreenStateTracker.kt`: registrado em `JancarApp.onCreate` (`registerReceiver` dinâmico, `ACTION_SCREEN_OFF` grava `lastScreenOffAt` em memória e no store; `ACTION_SCREEN_ON` consulta a política e, se verdadeiro, inicia `MainActivity` com extra `show_welcome=true` e `FLAG_ACTIVITY_NEW_TASK`). Em `MainActivity.onNewIntent`, extra `show_welcome` → `screen = Welcome`.
- Verificar no aparelho se o desligar/ligar do carro dispara esses broadcasts (`adb logcat -b events | grep -i screen`); se não, ajustar o gatilho (spec Fase 4, item 2).

## T4.3 Relógio
`ui/home/ClockState.kt`: `@Composable fun rememberClock(): State<ClockInfo>` com `DisposableEffect` registrando `BroadcastReceiver` para `ACTION_TIME_TICK`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` e `onDispose { unregisterReceiver }`. Formatação pura e testável: `ClockFormatter.format(Instant/ZonedDateTime, Locale("pt","BR"))` → `("18:37", "Quinta-feira, 5 de novembro")` com primeira letra maiúscula. Teste unitário com datas fixas (usa `java.time`, API 26+, ok no minSdk 27).

## T4.4 Boas-vindas
`ui/welcome/WelcomeScreen.kt`: copiar `docs/reference/boot-background.jpg` → `app/src/main/res/drawable-nodpi/boot_background.jpg`; `Box(fillMaxSize().clickable { onContinue() })` com `Image(ContentScale.Crop)`, degradê (`Brush.verticalGradient` com stops 0/0.68/1 e cores do spec), coluna central (legenda, relógio, data) e rodapé (seta + texto). Acessibilidade: `contentDescription = "Toque para continuar"`.

## T4.5 Home
`ui/home/HomeScreen.kt` seguindo a ordem do spec §5.1: topo (`ClockBlock`, legenda, 3 `JIconButton`), `Spacer(weight 1f)`, `FlowRow`/`Row` de `QuickChip`, `Row` de 4 `MainTile` com `weight(1f)`. Padding 52/64/48 dp. `HomeViewModel` expõe ações: `openAuto/openMusic/openRadio/openVideo`, `openSettingsAction(action)`, `toggleTheme`. Helper `SystemSettingsOpener(ctx)`: tenta ação específica, depois `ACTION_SETTINGS`, depois toast — sempre via `try/catch`.
Teste de UI (instrumentado, opcional): `composeTestRule` clica em “Rádio” e verifica que o `LaunchController` fake recebeu `RADIO`.

## T4.6 Painel de brilho
`ui/home/BrightnessPanel.kt`: `Box` ancorado (`Alignment.TopEnd`, padding top 146 dp/end 64 dp), `Slider` próprio simples (10–100) em Compose foundation (draggable) ou `androidx.compose.material3.Slider` **apenas se a dependência valer o custo** (preferir implementação própria de ~30 linhas). `BrightnessController`: `apply(percent)` → se `Settings.System.canWrite(ctx)` grava `SCREEN_BRIGHTNESS` (0–255), senão `window.attributes.screenBrightness = percent/100f`. Manifesto: `WRITE_SETTINGS` + atalho no painel para `ACTION_MANAGE_WRITE_SETTINGS` se `!canWrite` (mostrar só se resolver). Persistir valor no `SettingsStore` e reaplicar em `onResume`. Teste unitário da conversão percent↔0–255 (limites 10 e 100).

## T4.7 Gaveta
`ui/drawer/DrawerScreen.kt`: `DrawerApp(target, icon, name, tag)` na ordem do preview; `DrawerViewModel` calcula `installedCount` com `AndroidPackageGateway` (Fase 3). Grid de 6 colunas (`LazyVerticalGrid` ou `Row`s fixas — são só 12 itens: usar `Column`/`Row` simples para economizar). Pacotes de VLC (`org.videolan.vlc`), NewPipe (`org.schabi.newpipe`), F-Droid (`org.fdroid.fdroid`), Lightning (`acr.browser.lightning`) são os conhecidos de apps comuns — **confirmar com `pm list packages` (Fase 0)**; Galeria/Arquivos/Bluetooth/Música BT/EQ/Veículo/Scrcpy/OBD pendem da Fase 0. Tile ausente = desabilitado.

## T4.8 Integração e `MainActivity`
Ligar tema (`SettingsStore`), `AppRoot`, `LaunchController` (de `JancarApp`), brilho em `onResume`. Remover/ocultar botões de teste da Fase 3.

## T4.9 Verificação
`./gradlew testDebugUnitTest assembleDebug`; AVD 1024×600: percorrer as 3 telas, alternar tema, abrir painel de brilho; comparar com `app/Jancar launcher — preview visual.html` (abrir no navegador em 1024×600 ao lado). Aparelho real: blocos e chips abrem os destinos certos; app inexistente só dá toast. Commit: `feat: telas welcome, home e gaveta`.
