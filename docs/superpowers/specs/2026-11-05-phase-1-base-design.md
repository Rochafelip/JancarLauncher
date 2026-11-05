# Fase 1 — Base do projeto: Compose + HOME (spec)

Mestre: `2026-11-05-jancar-launcher-design.md` §3, §4, §7.

## Objetivo
Transformar o esqueleto do Studio num app Compose que o Android reconhece como um launcher (HOME), já com a moldura de tela correta, mostrando um texto de teste.

## Estado de partida
AGP 9.2.1, `compileSdk 36`, `minSdk 27`, deps só appcompat/material/core-ktx, manifesto sem `<activity>`, sem Kotlin de produção. Tema `Theme.JancarLauncher` baseado em Material (XML).

## Requisitos
1. Compose habilitado; `minSdk 27` mantido. Dependências mínimas: Compose BOM, `ui`, `foundation`, `activity-compose`, `lifecycle-runtime-compose`/`viewmodel-compose`, `datastore-preferences` (usada na fase 2). **Sem Material3** (decisão de memória, spec §8).
2. `MainActivity` (ComponentActivity) com intent-filters `MAIN` + `HOME` + `DEFAULT` e `LAUNCHER` (este facilita testar pelo emulador/adb).
3. Atributos: `launchMode=singleTask`, `stateNotNeeded=true`, `screenOrientation=landscape`, `configChanges="orientation|screenSize|keyboardHidden|uiMode|density"` (central não gira; evita recriação), `excludeFromRecents=true`.
4. Tema da janela escuro (`windowBackground #0a0b0f`) para não piscar branco; ActionBar desligada.
5. Tela imersiva: esconder barras do sistema, se existirem (a central tem barra própria? **confirmar no aparelho**; padrão: não esconder, só reservar insets).
6. Remover dependências sem uso (appcompat, material) quando nada mais as referenciar.
7. Permissão `RECEIVE_BOOT_COMPLETED` declarada já agora (receiver vem na fase 6).

## Fora de escopo
Qualquer tela real, tema Compose, lançamento de apps.

## Decisões minhas (revisar)
- Sem Material3.
- Tema XML da janela baseado em `android:Theme.Material.NoActionBar` (plataforma, sem lib).
- Compose compiler via plugin `org.jetbrains.kotlin.plugin.compose`; confirmar com AGP 9.x se Kotlin já vem embutido (se der conflito de plugin, seguir a mensagem do Gradle).

## Aceite
- `./gradlew assembleDebug` e `./gradlew testDebugUnitTest` passam.
- No emulador API 27 (1024×600, 160 dpi) o app abre mostrando o texto de teste sem faixa branca.
- `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` lista a MainActivity entre as opções (sem ainda defini-la como padrão).
- Girar o emulador não recria a Activity.
