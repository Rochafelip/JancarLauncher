# Player em todos os temas da Home — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Todo tema da Home com uma área de player sempre visível (música ou rádio), com ⏮ ⏯ ⏭, e atalhos Música/Rádio quando nada toca.

**Architecture:** Regra pura `PlayerModel` decide o que mostrar (faixa, rádio, ocioso) a partir do `NowPlayingState`, que ganha `isRadio` (serviço `IMedia`, `mediaType == 1`). Dois composables (`PlayerCard`, `PlayerBar`) em `ui/home/Player.kt` usam a regra; cada arranjo de `HomeLayouts.kt` chama um deles pelo `HomeParts`. Uma prévia de debug renderiza a Home com mídia falsa para capturar os 12 temas.

**Tech Stack:** Kotlin, Jetpack Compose (foundation+ui), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-17-player-temas-design.md`

## Global Constraints

- minSdk 27; Compose só foundation+ui; cores do `JTheme`; superfícies translúcidas sobre a foto (`OnPhotoSurfaces`/`LocalJancarColors` já aplicado pela `HomeScreen`).
- Toque: botões de transporte **56dp** no Cartão, **48dp** na Barra; atalhos Música/Rádio ≥ 48dp.
- Cartão ~112dp de altura; Barra 64dp. Larguras por tema na tabela do spec.
- Nunca abrir `IRadio`/serviço próprio do rádio; só o `IMedia` que o app já escuta.
- Textos: "Nada tocando", "Música", "Rádio", "Faixa sem nome".
- Testes: `./gradlew :app:testDebugUnitTest --tests "<classe>"`; build: `./gradlew :app:assembleDebug`. Trabalhar em `../JL-player` (branch `feat/player-temas`).

## Review Focus

1. Faixa com título enorme → uma linha com reticências, botões nunca empurrados para fora (teste visual na Task 4 com título de 80 caracteres).
2. Tema claro sobre a foto → texto e ícones do player legíveis (captura no tema claro na Task 4).
3. Rádio sem texto (só `mediaType = 1`) → título "Rádio", nunca vazio (teste na Task 2).
4. Música pausada com faixa → mostra a faixa com ⏯ em "tocar", não o ocioso (teste na Task 2).
5. Sessão padrão (Bluetooth) com título e sem artista → subtítulo some, sem linha vazia ocupando espaço (teste na Task 2).

---

### Task 1: Rádio no estado de "tocando agora" + registro

**Files:**
- Modify: `media/NowPlayingUi.kt` (`NowPlayingState.isRadio: Boolean = false`, incluir no equals/hashCode), `media/JancarNowPlaying.kt` (`map` preenche `isRadio = info.mediaType == RADIO_TYPE`, `const val RADIO_TYPE = 1`), `media/JancarMediaClient.kt` (construtor `log: RingLog? = null`; em cada `onMediaChange` diferente do anterior: `log?.add("midia", "tipo=${mediaType} t1=${text1} t2=${text2}")`), `JancarApp.kt` (`JancarMediaClient(this, log)`)
- Test: `media/JancarNowPlayingTest.kt`

**Interfaces:**
- Produces: `NowPlayingState.isRadio`; `JancarNowPlaying.RADIO_TYPE`.

- [ ] **Step 1: Teste que falha:** `map_radioType_setsIsRadio`: `JancarMediaInfo(mediaType = 1, text1 = "FM 99.1", …)` → `isRadio == true`, `title == "FM 99.1"`; `mediaType = 0` → `isRadio == false`.
- [ ] **Step 2:** Rodar → FAIL. **Step 3:** Implementar. **Step 4:** PASS (suíte toda).
- [ ] **Step 5:** Commit `feat(midia): reconhece o rádio no serviço de mídia da central`.

### Task 2: Regra do player

**Files:**
- Create: `ui/home/PlayerModel.kt`
- Test: `ui/home/PlayerModelTest.kt`

**Interfaces:**
- Produces: `enum class PlayerMode { Track, Radio, Idle }`; `data class PlayerView(val mode: PlayerMode, val title: String, val subtitle: String?, val playing: Boolean)`; `object PlayerModel { fun of(np: NowPlayingState): PlayerView }`.

Regras, nesta ordem (texto branco conta como nulo):
1. `isRadio` → `Radio`; título = `title` ou "Rádio"; subtítulo = "Rádio" (+ " · " + `artist` se houver), mas `null` quando o título já é "Rádio" e não há `artist`.
2. sem `title` e sem `artist` → `PlayerView(Idle, "Nada tocando", null, false)`.
3. senão → `Track`; título = `title` ou "Faixa sem nome"; subtítulo = `artist`.
`playing = isPlaying` (no ocioso, `false`).

- [ ] **Step 1: Testes que falham:** `empty_isIdle`; `blankTitleAndArtist_isIdle` ("  ", null); `trackWithArtist` → `Track`, "Ela", "Banda"; `pausedTrack_isStillTrack` (`isPlaying=false` → `Track`, `playing=false`) (Review Focus 4); `titleOnly_noSubtitle` → `subtitle == null` (Review Focus 5); `artistOnly_isTrackWithoutName` → "Faixa sem nome", "Banda"; `radioWithText` → `Radio`, "FM 99.1", "Rádio"; `radioWithRds` (artist "Rádio Cidade") → "Rádio · Rádio Cidade"; `radioWithoutText` (isRadio, nulls) → `Radio`, "Rádio", `null` (Review Focus 3).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(home): regra do player (faixa, rádio, nada tocando)`.

### Task 3: Componentes Cartão e Barra + prévia

**Files:**
- Create: `ui/home/Player.kt`, debug `HomePreviewActivity.kt` (+ registro no `src/debug/AndroidManifest.xml`)
- Modify: `ui/home/HomeLayouts.kt` (`HomeParts`: `fun Player(card: Boolean, modifier: Modifier)` que chama `PlayerCard`/`PlayerBar` com `nowPlaying`, `onPlay/onPause/onNext/onPrevious`, `onOpenMusic = apps[1].onClick`, `onOpenRadio = apps[2].onClick`)

**Interfaces:**
- Consumes: `PlayerModel.of` (Task 2), `TransportButton` (já existe no `NowPlayingCard.kt`).
- Produces: `@Composable fun PlayerCard(np: NowPlayingState, onPlay, onPause, onNext, onPrevious, onOpenMusic, onOpenRadio, modifier)`; `@Composable fun PlayerBar(...)` (mesmos parâmetros); `HomeParts.Player(card: Boolean, modifier: Modifier = Modifier)`.

- Cartão: capa 72dp (capa da faixa, senão ícone `NoteMusic` ou `Radio`), título `nowPlayingTitle` 1 linha, subtítulo `nowPlayingArtist` 1 linha (some se nulo), ⏮ ⏯ ⏭ de 56dp. Ocioso: ícone ♪, "Nada tocando", e no lugar do transporte dois botões **Música** · **Rádio** (56dp de altura).
- Barra: altura 64dp, ícone/capa 44dp, "título · subtítulo" numa linha com reticências, ⏮ ⏯ ⏭ de 48dp. Ocioso: "Nada tocando" + **Música** · **Rádio** (48dp).
- Toque na área de texto: abre o app da fonte (rádio → `onOpenRadio`; senão `onOpenMusic`).
- `HomePreviewActivity` (só debug): `--es layout <HomeLayout>` `--es media none|music|radio|long` `--ez light true`; renderiza `HomeScreen` com `NowPlayingState` falso (long = título de 80 caracteres).

- [ ] **Step 1:** Implementar `Player.kt` e `HomeParts.Player`. **Step 2:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Prévia no emulador com o tema D (já usa o card) trocado para `Player(card = true)`: capturas `media none|music|radio|long` conferidas (texto numa linha, botões no lugar).
- [ ] **Step 4:** Commit `feat(home): player em cartão e barra, com prévia de debug`.

### Task 4: Player em cada tema

**Files:**
- Modify: `ui/home/HomeLayouts.kt` (os 12 arranjos, conforme a tabela do spec; `MusicOrIdle` e os usos de `NowPlaying` trocados por `Player`; `veil()` onde o player entra sobre a foto: G, H, K, L), `ui/home/HomeLayout.kt` (miniaturas: um bloco a mais com o lugar do player em cada tema), `ui/home/NowPlayingCard.kt` (o card antigo sai se ninguém mais usar; `TransportButton` fica)
- Test: `ui/home/HomeLayoutTest.kt` (se houver teste das miniaturas, atualizar)

- [ ] **Step 1:** Trocar os 12 arranjos. I Widgets: data do relógio numa linha só (fonte menor ou `maxLines = 1` com o dia abreviado).
- [ ] **Step 2:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → todos PASS.
- [ ] **Step 3:** Capturas no emulador dos 12 temas × (`none`, `music`, `radio`) no escuro e os 12 × `music` no claro, montadas em folhas; conferir: player sempre visível, nada sobreposto, carro da foto ainda visível, texto legível, botões ≥ 48dp. Corrigir o que falhar.
- [ ] **Step 4:** Commit `feat(home): player de música ou rádio em todos os temas`.

### Task 5: Documentação e versão

**Files:**
- Modify: `app/CLAUDE.md` (seção da Home: player em todos os temas, tabela de lugares, rádio pelo `mediaType`, teste no carro), `app/build.gradle.kts` (`versionCode = 17`, `versionName = "1.9.0"`)

- [ ] **Step 1:** Atualizar; `./gradlew :app:testDebugUnitTest :app:assembleDebug` → PASS.
- [ ] **Step 2:** Commit `chore: versao 1.9.0 (versionCode 17)`.
