# Ideias do Rota (diagnóstico, viagens, ajustes, custo, bateria, velocidade, cor) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar ao JancarLauncher relatório de erros, tela de Ajustes, atualização sem ADB, bateria e motor
esquentando pelo OBD, scanner de falhas, custo e histórico de viagens, aviso de velocidade, retomar música e cor do
Fox.

**Architecture:** Regras puras e testáveis (parsers, regras de aviso, viagens, custo, cor) em arquivos pequenos; o
`ObdClient` ganha uma fila de comandos sobre o socket único do ELM327; monitores no escopo do `JancarApp`
(`HealthMonitor`, `SpeedAlarm`, `TripRecorder`) alimentam o Painel, a tela Fox, o cartão sobre o AA e duas telas
novas (`Screen.Obd`, `Screen.Settings`). Persistência: DataStore para escolhas, JSON em `filesDir` para listas.

**Tech Stack:** Kotlin, Jetpack Compose (foundation+ui, sem Material3), DataStore, coroutines, JUnit 4,
`org.json` (Android; nos testes via `testImplementation`).

**Spec:** `docs/superpowers/specs/2026-11-17-custo-bateria-velocidade-cor-design.md` e
`docs/superpowers/specs/2026-11-17-diagnostico-viagens-ajustes-design.md` (ler os dois).

## Global Constraints

- minSdk 27: tudo precisa funcionar no Android 8.1 da central; 1024×600 @160; 2 GB de RAM; sem root.
- Compose só `foundation`+`ui` (sem Material3); cores do `JTheme`; toque mínimo 48dp, botões +/− de 56dp.
- OBD **somente leitura**: só `AT*` de configuração, `ATRV`, modo 01 e modos 03/07/0A. Nunca 04.
- Um só socket do ELM327 (o do `ObdClient`); nunca um segundo `connect()` no `CanBusClient`/`JancarCarClient` para
  ler dados (usar os `StateFlow` que já existem).
- Sem dado = "—" (ou "---" no visor da tela Fox); nada simulado fora das prévias/DEMO de debug.
- Painel limpo: bateria e água só como aviso (`DashModel.Alert`), nunca número fixo.
- Textos em português; nada copiado do Rota (imagens, tabela de códigos, textos longos).
- Requisição ao GitHub sem nenhum dado do dono (User-Agent genérico `JancarLauncher-updater`).
- Testes: `./gradlew :app:testDebugUnitTest --tests "<classe>"`; build: `./gradlew :app:assembleDebug`.
- Trabalhar no branch `feat/ideias-rota` (criado na Task 1); commits pequenos, mensagem em português no estilo do repo
  (`feat(obd): ...`), terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Ignição desligada no meio de uma leitura de falhas → a leitura termina com erro, o loop do OBD volta, a tela
   mostra "Não foi possível concluir a leitura" (teste na Task 10).
2. Central suspensa a noite toda com viagem aberta → a viagem fecha com o fim no último dado recebido, não na hora em
   que a central acordou (teste na Task 13).
3. Preço nunca definido / limite desligado → nenhum R$ além de "definir preço", nenhum bipe (testes nas Tasks 12 e 15).
4. GitHub fora do ar, limite de requisições ou release sem `.apk` → mensagem, sem crash (teste na Task 3).
5. Máscara da lataria ausente no APK → sem fileira de cores, foto como hoje (teste na Task 18).

---

## Fase 1 — Relatório de erros e Ajustes

### Task 1: Registro de eventos e gravação de crashes

**Files:**
- Create: `app/src/main/java/com/rocha/jancarlauncher/system/RingLog.kt`, `system/CrashStore.kt`
- Modify: `boot/CrashRestarter.kt`, `JancarApp.kt` (instância `val log = RingLog()`), `obd/ObdClient.kt` e
  `can/CanBusClient.kt` (uma linha de log em conectou/caiu/erro), `app/build.gradle.kts` (`testImplementation("org.json:json:20240303")`)
- Test: `app/src/test/java/com/rocha/jancarlauncher/system/RingLogTest.kt`, `system/CrashStoreTest.kt`

**Interfaces:**
- Produces: `class RingLog(capacity: Int = 200) { fun add(tag: String, msg: String, wallMs: Long = System.currentTimeMillis()); fun lines(): List<String> }` (linha = `"HH:mm:ss tag: msg"`, thread-safe);
  `data class CrashEntry(val file: File, val wallMs: Long, val firstLine: String)`;
  `class CrashStore(dir: File, keep: Int = 10) { fun write(wallMs: Long, version: String, error: Throwable, log: List<String>): File; fun list(): List<CrashEntry>; fun read(e: CrashEntry): String; fun clear() }`;
  `JancarApp.log: RingLog`, `JancarApp.crashes: CrashStore` (dir `filesDir/crash`).

- [ ] **Step 1:** `git switch -c feat/ideias-rota` e commit dos dois specs (`docs: specs das ideias do Rota`).
- [ ] **Step 2: Testes que falham**
  - `RingLogTest.keepsOnlyNewest200`: 205 `add` → `lines().size == 200`, primeira linha contém `"msg5"`.
  - `CrashStoreTest.writesVersionStackAndLog`: `write(…, "1.8.0", RuntimeException("boom"), listOf("a","b"))` → arquivo contém `"1.8.0"`, `"RuntimeException: boom"`, `"a"`, `"b"`.
  - `CrashStoreTest.keepsNewest10_sortedNewestFirst`: 12 escritas com `wallMs` 1..12 → `list().size == 10`, `list().first().wallMs == 12`.
  - `CrashStoreTest.clearRemovesAll`: `clear()` → `list().isEmpty()`.
- [ ] **Step 3:** Rodar `./gradlew :app:testDebugUnitTest --tests "com.rocha.jancarlauncher.system.*"` → FAIL (classes não existem).
- [ ] **Step 4:** Implementar `RingLog` e `CrashStore` (nome do arquivo `crash-<wallMs>.txt`; `firstLine` = primeira linha da pilha). `CrashRestarter.install` grava com `app.crashes.write(...)` **antes** de reabrir, dentro do `try` que já existe.
- [ ] **Step 5:** Rodar os testes → PASS; `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit `feat(sistema): registro de eventos e crashes gravados no aparelho`.

### Task 2: Tela Ajustes do launcher com Relatório de erros

**Files:**
- Create: `ui/settings/SettingsScreen.kt`, `ui/settings/CrashReportScreen.kt`
- Modify: `ui/nav/Screen.kt` (`Settings`), `ui/nav/ScreenRestore.kt`, `ui/AppRoot.kt` (rota + voltar → Home), `ui/home/BrightnessPanel.kt` (linha "Ajustes do launcher" abaixo de "Escolher tema", `onOpenSettings`)
- Test: `ui/nav/ScreenRestoreTest.kt`

**Interfaces:**
- Consumes: `JancarApp.crashes`, `JancarApp.log` (Task 1).
- Produces: `@Composable fun SettingsScreen(onBack: () -> Unit, sections: List<@Composable () -> Unit>)` — cada fase seguinte adiciona uma seção (Atualizar, Retomar música); `@Composable fun CrashReportSection()`.

- [ ] **Step 1: Teste que falha:** `ScreenRestoreTest.settingsRestoresToHome`: `restore("Settings") == Screen.Home`.
- [ ] **Step 2:** Rodar → FAIL.
- [ ] **Step 3:** `Screen.Settings` + regra em `ScreenRestore`. Tela no estilo da gaveta (`PhotoBackdrop` + cartões). Seções: **Relatório de erros** (lista data + `firstLine`; toque → texto completo rolável em 14sp; "Apagar tudo" pede dois toques, como o ZERAR; "Registro agora" mostra `log.lines()`), **Versão instalada** (`BuildConfig.VERSION_NAME`).
- [ ] **Step 4:** Testes → PASS; build ok; no emulador abrir pelo painel de brilho e ver um crash forçado (debug: `adb shell am crash com.rocha.jancarlauncher` gera um crash que deve aparecer na lista).
- [ ] **Step 5:** Commit `feat(ajustes): tela Ajustes do launcher com relatório de erros`.

## Fase 2 — Atualizar sem ADB

### Task 3: Regras puras da atualização

**Files:**
- Create: `system/update/UpdateRules.kt`
- Test: `system/update/UpdateRulesTest.kt`

**Interfaces:**
- Produces: `data class Release(val tag: String, val apkUrl: String)`; `object UpdateRules { fun parseLatest(json: String): Release?; fun isNewer(remoteTag: String, installedName: String): Boolean; fun verdict(archive: ApkInfo?, installed: ApkInfo): Verdict }`; `data class ApkInfo(val pkg: String, val versionCode: Long, val certSha256: Set<String>)`; `enum class Verdict { Ok, Unreadable, OtherPackage, OtherSignature, NotNewer }`.

- [ ] **Step 1: Testes que falham**
  - `parseLatest_picksApkAsset`: JSON com `"tag_name":"v1.8.0"` e assets `notes.txt` e `app-release.apk` → `Release("v1.8.0", <url do .apk>)`.
  - `parseLatest_noApk_isNull` e `parseLatest_garbage_isNull` (`"{"`, `""`, `{"message":"API rate limit exceeded"}`).
  - `isNewer`: `("v1.8.0","1.7.3") == true`, `("v1.7.3","1.7.3") == false`, `("v1.7.10","1.7.9") == true`, `("vX","1.7.3") == false`.
  - `verdict`: `null` → `Unreadable`; pacote diferente → `OtherPackage`; certificado diferente → `OtherSignature`; `versionCode` igual ou menor → `NotNewer`; senão `Ok`.
- [ ] **Step 2:** Rodar → FAIL. **Step 3:** Implementar com `org.json`. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(atualizacao): regras de versão e assinatura`.

### Task 4: Baixar/escolher e instalar o APK

**Files:**
- Create: `system/update/Updater.kt`, `ui/settings/UpdateSection.kt`, `app/src/main/res/xml/update_paths.xml`
- Modify: `AndroidManifest.xml` (`INTERNET`, `REQUEST_INSTALL_PACKAGES`, `READ_EXTERNAL_STORAGE`, `FileProvider` `${applicationId}.update`), `ui/settings/SettingsScreen` (seção), `docs/INSTALL.md` (passo `adb shell appops set com.rocha.jancarlauncher REQUEST_INSTALL_PACKAGES allow` e `pm grant ... READ_EXTERNAL_STORAGE`)

**Interfaces:**
- Consumes: `UpdateRules` (Task 3), `JancarApp.log`.
- Produces: `class Updater(context: Context) { suspend fun checkGitHub(): Release?; suspend fun download(r: Release, onProgress: (Int) -> Unit): File?; fun inspect(apk: File): ApkInfo?; fun installed(): ApkInfo; fun install(apk: File) ; suspend fun copyFromUri(uri: Uri): File?; fun findOnStorage(): List<File> }`.

- [ ] **Step 1:** Implementar `Updater`: URL `https://api.github.com/repos/Rochafelip/JancarLauncher/releases/latest`, `HttpURLConnection` com timeout 10 s; APK em `cacheDir/update/update.apk`; `inspect` via `getPackageArchiveInfo(path, GET_SIGNATURES)` (SHA-256 de cada assinatura); `install` com `Intent.ACTION_INSTALL_PACKAGE` + `FileProvider` + `FLAG_GRANT_READ_URI_PERMISSION`; `findOnStorage` procura `*.apk` até 2 níveis em `/storage/*` (fora `emulated/0/Android`).
- [ ] **Step 2:** Seção "Atualizar o launcher": "Procurar no GitHub" → "Versão X disponível · Baixar" / "Você já tem a mais nova" / "Sem internet ou GitHub fora do ar"; barra de progresso; "Instalar de um arquivo" → `ACTION_OPEN_DOCUMENT` (`application/vnd.android.package-archive`), e se não houver app que atenda, a lista de `findOnStorage()`. Sempre `UpdateRules.verdict` antes de instalar; mensagem por motivo (`OtherSignature` = "APK assinado com outra chave").
- [ ] **Step 3:** Verificar no emulador: build com `versionCode` +1 copiado com `adb push app-debug.apk /sdcard/Download/` → "Instalar de um arquivo" → tela do Android de instalação → app reabre com a versão nova e preferências mantidas. Um APK de outro app → recusado com "APK de outro aplicativo".
- [ ] **Step 4:** Commit `feat(atualizacao): atualizar pelo GitHub ou por arquivo, sem ADB`.

## Fase 3 — Fila do OBD, bateria e motor esquentando

### Task 5: Fila de comandos e ATRV no ObdClient

**Files:**
- Modify: `obd/ObdClient.kt`, `obd/ElmProtocol.kt`
- Test: `obd/ObdMathTest.kt` (parser), `obd/ObdExclusiveTest.kt`

**Interfaces:**
- Produces: `interface ElmLink { fun send(cmd: String, timeoutMs: Long = 1_500): String }` (o `ElmIo` atual passa a implementá-la); `ElmProtocol.parseVoltage(raw: String): Float?`; `const val ATRV_OFFSET_V = 0f`; `ObdClient.Live` ganha `batteryV: Float?` e `rpm: Float?`; `suspend fun <T> ObdClient.exclusive(block: (ElmLink) -> T): T?` (null se desconectado; roda entre dois ciclos do loop sob o mesmo `Mutex`).

- [ ] **Step 1: Testes que falham:** `parseVoltage("12.6V") == 12.6f`, `("14.1 V\r") == 14.1f`, `("?") == null`, `("") == null`, `("3.0V") == null` e `("25.0V") == null` (fora de 5..20 V = leitura ruim); `ObdExclusiveTest.blockRunsBetweenCycles` com um `ElmLink` falso que registra comandos: o bloco do `exclusive` nunca intercala com os comandos de um ciclo.
- [ ] **Step 2:** Rodar → FAIL.
- [ ] **Step 3:** Extrair o corpo do `while` para `cycle(link)` sob um `Mutex`; `exclusive` pega o mesmo `Mutex`. `ATRV` a cada 5 s (como a água a cada 2 s), somando `ATRV_OFFSET_V`. `rpm` lido a cada 1 s quando não for MAP (no MAP já vem a cada ciclo). Log no `RingLog` quando a tensão some/volta.
- [ ] **Step 4:** PASS; build ok. **Step 5:** Commit `feat(obd): fila de comandos e tensão da bateria (ATRV)`.

### Task 6: Regras de bateria e de motor esquentando + HealthMonitor

**Files:**
- Create: `obd/BatteryRule.kt`, `obd/OverheatRule.kt`, `obd/HealthMonitor.kt`
- Modify: `JancarApp.kt` (`val health: HealthMonitor`), `ui/dash/DashModel.kt` (alertas), `ui/dash/DashScreen.kt` (textos nas duas formas de aviso, linhas ~547 e ~647), `ui/car3d/FoxCluster.kt` (luz da bateria), `ui/car3d/CarScreen.kt`, `overlay/OverlayPolicy.kt` + `overlay/AutoOverlay.kt` (linha de aviso), `ui/icons/Icons.kt` (`Battery`)
- Test: `obd/BatteryRuleTest.kt`, `obd/OverheatRuleTest.kt`, `ui/dash/DashModelTest.kt`, `overlay/OverlayPolicyTest.kt`

**Interfaces:**
- Consumes: `ObdClient.Live.batteryV`, `.rpm`, `.coolantC` (Task 5).
- Produces: `enum class BatteryAlert { NotCharging, HighVoltage, Weak }`; `class BatteryRule { fun update(volts: Float?, rpm: Float?, nowMs: Long): BatteryAlert? }`; `class OverheatRule { fun update(coolantC: Float?, nowMs: Long): Boolean }`; `data class DashModel.Health(val battery: BatteryAlert? = null, val overheat: Boolean = false, val coolantC: Float? = null, val batteryV: Float? = null)`; `DashModel.alerts(r, mode, health: Health = Health())`; novos `Alert`: `Overheat(true)`, `BatteryNotCharging(true)`, `BatteryHigh(true)`, `BatteryWeak(false)`; `class HealthMonitor(obd: StateFlow<ObdClient.Live>, scope: CoroutineScope) { val health: StateFlow<DashModel.Health> }`; `OverlayPolicy.lines(r, live, mode, gear, health)` com `Lines.warning: String?`.

- [ ] **Step 1: Testes que falham** (`nowMs` em ms; valores do spec):
  - `BatteryRule`: giro 800 desde t=0, 12,4 V de t=31 s a t=51 s → `NotCharging` em t=51 s e `null` em t=50 s; 15,2 V por 20 s com motor ligado há 31 s → `HighVoltage`; giro 0 e 12,0 V por 20 s → `Weak`; motor ligou em t=0 com 11,0 V até t=9 s → nada (partida); sai do `NotCharging` só com ≥ 13,3 V; `volts == null` → `null` e zera os tempos.
  - `OverheatRule`: 111 °C por 10 s → `true`; 9,9 s → `false`; ligado e cai para 106 → continua `true`; 104 → `false`; `null` → `false`.
  - `DashModelTest.alerts_orderWithHealth`: com `Shift`, `Overheat`, `BatteryNotCharging`, porta aberta andando e reserva → ordem `Shift, Overheat, BatteryNotCharging, DoorOpen, LowFuel`; `BatteryWeak` fica depois de `LowFuel`.
  - `OverlayPolicyTest.overheatWarningLine`: `health.overheat = true, coolantC = 112f` → `warning == "ÁGUA 112 °C"`; sem aviso → `null`.
- [ ] **Step 2:** Rodar → FAIL. **Step 3:** Implementar. Textos: "MOTOR ESQUENTANDO" + "112 °C"; "BATERIA NÃO CARREGA" + "12,4 V com o motor ligado"; "TENSÃO ALTA" + "15,2 V"; "BATERIA FRACA" + "12,0 V". Tela Fox: luz vermelha da bateria no conta-giros só com `battery != null` (desenhada como a da lanterna, `AnalogGauge(..., battery: Boolean? = null)`). Cartão sobre o AA: linha vermelha `warning`. Prévias: `DashPreviewActivity --ez hot true` e `--es battery NotCharging`.
- [ ] **Step 4:** PASS; conferir as prévias no emulador. **Step 5:** Commit `feat(painel): avisos de bateria e motor esquentando pelo OBD`.

## Fase 4 — Scanner de falhas

### Task 7: Parser de DTC e da luz de injeção

**Files:**
- Create: `obd/Dtc.kt`
- Test: `obd/DtcTest.kt`

**Interfaces:**
- Produces: `data class MilStatus(val milOn: Boolean, val count: Int)`; `object Dtc { fun parse(raw: String, mode: Int): List<String>?; fun milStatus(raw0101: String): MilStatus?; fun code(a: Int, b: Int): String }` (`null` = erro/sem resposta do adaptador; lista vazia = sem códigos).

O ELM está com `ATS0`/`ATH0` (sem espaços, sem cabeçalho). Algoritmo do `parse`:

```
limpa \r, espaços e '>'; "NODATA" → []; "?", "ERROR", "UNABLETOCONNECT", "BUSINIT...ERROR" → null
linhas com "N:" (multiquadro CAN) → junta os dados na ordem de N e descarta a linha de tamanho ("00A")
para cada resposta que começa com (0x40+mode) em hex ("43", "47", "4A"):
    bytes = o resto em pares hex
    é CAN se veio em multiquadro ou se o número de bytes é ímpar (KWP sempre traz 6 bytes = 3 códigos):
        o 1º byte é a contagem n; usa só os n pares seguintes
    pares (a,b) ≠ (00,00) → code(a,b)
junta tudo sem repetir, na ordem
code(a,b): letra = "PCBU"[a shr 6]; dígito1 = (a shr 4) and 3; depois a and 0xF e b em hex maiúsculo
```

- [ ] **Step 1: Testes que falham:** `"43013302010000"` (KWP) → `[P0133, P0201]`; `"430201330301"` (CAN) → `[P0133, P0301]`; `"00A\r0:430401330301\r1:04200171000000"` → `[P0133, P0301, P0420, P0171]`; `"NO DATA"` → `[]`; `"43"` → `[]`; `"?"` → `null`; `code(0x41,0x23) == "C0123"`, `code(0x81,0x00) == "B0100"`, `code(0xC1,0x00) == "U0100"`; modo 7 `"47010000000000"` → `[P0100]`; duas ECUs `"430101330000\r430103010000"` → `[P0133, P0301]` sem repetir; `milStatus("410183076500") == MilStatus(true, 3)`; `milStatus("410100076500") == MilStatus(false, 0)`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS. **Step 5:** Commit `feat(obd): leitura de códigos de falha (modos 03/07/0A)`.

### Task 8: Tabela de códigos em português

**Files:**
- Create: `app/src/main/assets/obd/dtc_pt.json`, `obd/DtcCatalog.kt`
- Test: `obd/DtcCatalogTest.kt`

**Interfaces:**
- Produces: `enum class Severity { Baixa, Media, Alta }`; `data class DtcInfo(val code: String, val title: String, val severity: Severity, val causes: List<String>, val checks: List<String>, val known: Boolean)`; `class DtcCatalog(json: String) { fun describe(code: String): DtcInfo }`. JSON: `{"P0301": {"t": "...", "s": "alta", "c": ["..."], "v": ["..."]}, ...}`.

- [ ] **Step 1: Testes que falham:** o arquivo real (lido de `src/main/assets` no teste) tem ≥ 120 códigos, todos casando `^P0[0-9A-F]{3}$`, com título, gravidade válida, ≥ 1 causa e ≥ 1 verificação; `describe("P0301").severity == Alta`; `describe("P0999").known == false` e título "Código genérico sem descrição nesta versão"; `describe("P1234").title` contém "fabricante"; `describe("U0100").known == false`.
- [ ] **Step 2:** FAIL.
- [ ] **Step 3:** Escrever a tabela **com texto próprio**, frases curtas para ler parado. Faixas obrigatórias: P0010–P0024 (comando), P0100–P0199 (ar/combustível/sensores, incluindo P0105–P0108, P0110–P0118, P0120–P0123, P0130–P0141, P0171–P0175), P0200–P0204 e P0217, P0230, P0261–P0272 (injetores 1–4), P0300–P0304 (falha de combustão), P0325–P0344 (detonação, rotação, fase), P0351–P0354 (bobinas), P0400–P0499 principais (EGR, catalisador P0420/P0421, EVAP P0440–P0456), P0500–P0507 (velocidade, marcha lenta), P0560–P0563 (tensão), P0600–P0606 (módulo). Gravidade: falha de combustão, superaquecimento, pressão de óleo e rotação = alta; sondas, mistura, catalisador = média; EVAP e tampa = baixa. `P1`/`P3` = fabricante; `P2` e `B/C/U` fora da tabela = genérico sem descrição.
- [ ] **Step 4:** PASS. **Step 5:** Commit `feat(obd): tabela própria de códigos em português`.

### Task 9: Histórico de leituras

**Files:**
- Create: `obd/ScanHistory.kt`
- Test: `obd/ScanHistoryTest.kt`

**Interfaces:**
- Produces: `data class ScanResult(val wallMs: Long, val mil: MilStatus?, val confirmed: List<String>, val pending: List<String>, val permanent: List<String>?, val error: String? = null)`; `class ScanHistory(dir: File, keep: Int = 10) { fun add(r: ScanResult); fun list(): List<ScanResult> }` (arquivo `dir/scans.json`, mais nova primeiro).

- [ ] **Step 1:** Testes: 12 `add` → 10 itens, mais nova primeiro; `permanent = null` sobrevive à ida e volta do JSON; arquivo corrompido → `list() == emptyList()` e o próximo `add` regrava.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS. **Step 5:** Commit `feat(obd): histórico das leituras de falhas`.

### Task 10: `ObdClient.scan()`

**Files:**
- Modify: `obd/ObdClient.kt`
- Create: `obd/Scanner.kt`
- Test: `obd/ScannerTest.kt`

**Interfaces:**
- Consumes: `exclusive`/`ElmLink` (Task 5), `Dtc` (Task 7), `ScanResult` (Task 9).
- Produces: `object Scanner { fun run(link: ElmLink, wallMs: Long): ScanResult }`; `suspend fun ObdClient.scan(): ScanResult` (desconectado → `ScanResult(error = "OBD não conectado")`); `object ScanGate { fun allowed(obdSpeedKmh: Float?): Boolean }` (só com velocidade lida e ≤ 3 km/h).

- [ ] **Step 1: Testes que falham** com `ElmLink` falso que responde por comando: `0101`→`"410183076500"`, `03`→`"430201330301"`, `07`→`"NO DATA"`, `0A`→`"?"` → `ScanResult(mil=MilStatus(true,3), confirmed=[P0133,P0301], pending=[], permanent=null)`; link que lança `IOException` no `07` → `error == "Não foi possível concluir a leitura"` e `confirmed` mantém o que já leu; `ScanGate.allowed(null) == false`, `(2f) == true`, `(5f) == false`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar (timeout 6 s nos modos 03/07/0A; erro de IO derruba a sessão como hoje, e o loop reconecta). **Step 4:** PASS. **Step 5:** Commit `feat(obd): leitura de falhas pela fila do OBD`.

### Task 11: Tela OBD, aba Falhas

**Files:**
- Create: `ui/obd/ObdScreen.kt` (abas), `ui/obd/FaultsTab.kt`
- Modify: `ui/nav/Screen.kt` (`Obd`), `ui/nav/ScreenRestore.kt`, `ui/AppRoot.kt` (voltar → Dash), `ui/dash/DashScreen.kt` (botão `JIcons.Wrench` no topo, ao lado do de carro), `ui/icons/Icons.kt` (`Wrench`), `JancarApp.kt` (`scanHistory`, `dtcCatalog` lazy); debug: `ObdPreviewActivity` com resultado falso
- Test: `ui/nav/ScreenRestoreTest.kt` (`restore("Obd") == Screen.Home`)

**Interfaces:**
- Consumes: `ObdClient.scan`, `ScanGate`, `ScanHistory`, `DtcCatalog`.
- Produces: `@Composable fun ObdScreen(onBack: () -> Unit, tripsTab: @Composable () -> Unit)` (a aba Viagens chega na Task 14; até lá mostra "Em breve").

- [ ] **Step 1:** Teste do restore → FAIL → regra → PASS.
- [ ] **Step 2:** Aba Falhas: botão **LER FALHAS** (desabilitado com "Disponível com o carro parado" quando `!ScanGate.allowed`); estado lendo; resultado: luz de injeção acesa/apagada, cada código com etiqueta CONFIRMADO/PENDENTE/PERMANENTE e cor da gravidade (alta vermelho, média âmbar, baixa cinza); toque abre título, causas, verificações; aviso fixo "Um código indica a condição que a central detectou; não confirma que uma peça está com defeito."; "Últimas leituras" do histórico.
- [ ] **Step 3:** Conferir no emulador com `ObdPreviewActivity`. Build ok.
- [ ] **Step 4:** Commit `feat(obd): tela OBD com scanner de falhas`.

## Fase 5 — Custo e viagens

### Task 12: Custo da viagem em R$

**Files:**
- Create: `obd/TripCost.kt`, `ui/dash/PricePanel.kt`
- Modify: `obd/Trip.kt`, `obd/ObdClient.kt`, `settings/SettingsStore.kt`, `ui/dash/ConsumptionBlock.kt`
- Test: `obd/TripCostTest.kt`, `obd/ObdMathTest.kt`, `settings/SettingsStoreTest.kt`

**Interfaces:**
- Produces: `Trip(liters, km, ethanolLiters: Double = 0.0)` e `Trip.add(litersPerHour, speedKmh, dtSeconds, ethanol: Float)`; `object TripCost { fun ethanolWeight(f: Float): Double; fun reais(liters: Double, ethanolLiters: Double, gasCents: Int?, ethCents: Int?): Double?; fun text(reais: Double?): String; fun step(cents: Int, delta: Int): Int }`; `SettingsStore.gasPriceCents`, `ethanolPriceCents: Flow<Int?>` + setters; chave `trip_ethanol_liters`.

- [ ] **Step 1: Testes que falham:** `ethanolWeight(0.27f) == 0.0`, `(1f) == 1.0`, `(0.63f) ≈ 0.493`, `(0f) == 0.0`; `reais(10.0, 0.0, 619, 429) == 61.90`; `reais(10.0, 10.0, 619, 429) == 42.90`; `reais(10.0, 5.0, 619, 429) == 52.40`; qualquer preço `null` → `null`; `text(52.4) == "R$ 52,40"`, `text(null) == "R$ — definir preço"`; `step(619, +10) == 629`, `step(105, -10) == 100`, `step(1995, +10) == 2000` (limites R$ 1,00..20,00); `Trip.add` com etanol 1,0 soma `ethanolLiters == liters`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar; `ObdClient` passa a fração usada no intervalo (PID 52 ÷ 100, senão `blend.ethanol`); `ZERAR` zera os três. Linha de custo pequena no bloco VIAGEM; toque abre `PricePanel` (Gasolina e Etanol, botões −0,10 / − / + / +0,10 de 56dp, segurar repete a cada 150 ms, OK ou toque fora fecha).
- [ ] **Step 4:** PASS; prévia `DashPreviewActivity --ez obd true`. **Step 5:** Commit `feat(painel): custo da viagem em reais`.

### Task 13: Gravador de viagens por ligada do carro

**Files:**
- Create: `obd/TripRecorder.kt`, `obd/TripStore.kt`
- Modify: `obd/ObdClient.kt` (alimenta o gravador a cada ciclo e na queda da sessão), `JancarApp.kt`
- Test: `obd/TripRecorderTest.kt`, `obd/TripStoreTest.kt`

**Interfaces:**
- Produces: `data class TripRecord(val startWallMs: Long, val endWallMs: Long, val movingS: Double, val km: Double, val liters: Double, val ethanolLiters: Double, val maxKmh: Float) { val avgKmh: Double?; val kmPerLiter: Double? }`; `class TripRecorder(graceMs: Long = 180_000) { fun sample(elapsedMs: Long, wallMs: Long, speedKmh: Float?, litersPerHour: Float?, ethanol: Float, dtS: Double): TripRecord?; fun disconnected(elapsedMs: Long, wallMs: Long); fun tick(elapsedMs: Long): TripRecord?; val current: TripRecord? }`; `class TripStore(dir: File, keep: Int = 20) { fun add(r: TripRecord); fun list(): List<TripRecord> }` (`dir/trips.json`).

- [ ] **Step 1: Testes que falham** (`elapsedMs` = `SystemClock.elapsedRealtime`, que conta durante a suspensão):
  - viagem só começa com velocidade > 0; `movingS` só soma com velocidade > 0.
  - desconecta e volta em 2 min → mesma viagem; volta em 4 min → `sample` devolve a viagem anterior fechada e começa outra.
  - desconecta às 22:00 e o próximo evento é `tick` 9 h depois → devolve a viagem com `endWallMs` = 22:00 (último dado), não a hora do `tick` (Review Focus 2).
  - viagem com 0,29 km → descartada (devolve `null`).
  - `avgKmh` = km ÷ (movingS/3600); `maxKmh` = maior velocidade.
  - `TripStore`: 22 `add` → 20, mais nova primeiro.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar; `tick` chamado a cada 30 s pelo `JancarApp`; registro fechado → `TripStore.add` + linha no `RingLog`.
- [ ] **Step 4:** PASS. **Step 5:** Commit `feat(obd): viagens separadas por ligada do carro`.

### Task 14: Aba Viagens

**Files:**
- Create: `ui/obd/TripsTab.kt`, `obd/TripText.kt`
- Modify: `ui/obd/ObdScreen.kt`
- Test: `obd/TripTextTest.kt`

**Interfaces:**
- Consumes: `TripRecorder.current`, `TripStore.list()`, `TripCost`, preços (Tasks 12–13).
- Produces: `object TripText { fun main(r: TripRecord, gasCents: Int?, ethCents: Int?): String; fun speeds(r: TripRecord): String; fun whenText(r: TripRecord, nowWallMs: Long): String }`.

- [ ] **Step 1: Testes que falham:** 12,4 km, 18 min andando, 0,94 L, sem etanol, gasolina 619 → `main == "12,4 km · 18 min · 13,2 km/L · R$ 5,82"`; sem preço → sem o trecho do R$; `speeds == "média 41 km/h · máx 98 km/h"`; litros < 0,05 → km/L "—"; `whenText` "hoje 08:12" / "ontem 18:40" / "14/11 07:55".
- [ ] **Step 2:** FAIL. **Step 3:** Aba: cartão "Viagem atual" (ao vivo, se houver) e lista das anteriores. **Step 4:** PASS. **Step 5:** Commit `feat(obd): aba de viagens anteriores`.

## Fase 6 — Aviso de velocidade

### Task 15: Regra, limite e bipe

**Files:**
- Create: `obd/SpeedAlertRule.kt`, `system/SpeedAlarm.kt`, `ui/dash/SpeedLimitPanel.kt`
- Modify: `settings/SettingsStore.kt` (`speedLimit: Flow<Int?>`), `JancarApp.kt`, `ui/dash/DashScreen.kt` (chip `LIMITE 110`/`SEM LIMITE` no topo; número vermelho), `overlay/OverlayPolicy.kt` + `AutoOverlay.kt` (velocidade vermelha)
- Test: `obd/SpeedAlertRuleTest.kt`, `overlay/OverlayPolicyTest.kt`

**Interfaces:**
- Produces: `enum class Beep { Double, Single }`; `class SpeedAlertRule { fun update(speedKmh: Float?, limit: Int?, nowMs: Long): Beep?; companion object { fun over(speedKmh: Float?, limit: Int?): Boolean } }` (`over` = ≥ limite + 2); `fun stepLimit(limit: Int?, delta: Int): Int?` (40..150 de 10 em 10; abaixo de 40 = `null`); `Lines.speedOver: Boolean`; `class SpeedAlarm(app: JancarApp)` (fonte: velocidade do `canBus.dashboard` quando `canBus.connected`, senão `obd.live.speedKmh`; `ToneGenerator(AudioManager.STREAM_MUSIC, 80)`).

- [ ] **Step 1: Testes que falham** (limite 110): 112 km/h por 1,9 s → `null`, por 2 s → `Double`; continua acima → `Single` aos 22 s e aos 42 s; cai para 106 e volta para 112 → sem novo `Double` (não rearmou), só os `Single` a cada 20 s enquanto ≥ 112; cai para 104 → rearma, e 112 por 2 s → `Double`; `limit = null` → sempre `null`; `speed = null` zera o tempo acima; `stepLimit(null, +10) == 40`, `stepLimit(40, -10) == null`, `stepLimit(150, +10) == 150`; `OverlayPolicyTest.speedOverTurnsRed`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar; velocidade do CAN com a mesma limpeza do `DashModel.reading` (MIN_VALUE = sem dado). Painel do limite: `[−] 110 km/h [+]` e `Desligar`. Log no `RingLog` a cada `Double`.
- [ ] **Step 4:** PASS; prévia `DashPreviewActivity --ez moving true` com limite 40 → bipe audível no emulador. **Step 5:** Commit `feat(painel): aviso sonoro acima do limite de velocidade`.

## Fase 7 — Retomar a música

### Task 16: Retomar a música ao ligar

**Pré-requisito (no carro, pelo dono):** ligar a central com música tocando e ver se ela retoma sozinha. Se retomar, **pular esta task** e registrar no `CLAUDE.md`.

**Files:**
- Create: `media/MusicResume.kt`, `ui/settings/MusicResumeSection.kt`
- Modify: `settings/SettingsStore.kt` (`resumeMusic: Flow<Boolean>` padrão `false`, `lastSource: Flow<String?>`), `ui/AppRoot.kt` (mesmo gatilho da Boas-vindas), `media/JancarNowPlaying.kt` (gravar a fonte que está tocando)
- Test: `media/MusicResumeTest.kt`

**Interfaces:**
- Produces: `sealed interface LastSource { object Jancar; data class Session(val pkg: String) }` com `encode()/decode(String?)`; `object MusicResume { fun shouldResume(enabled: Boolean, last: LastSource?, foregroundPkg: String?, playingNow: Boolean): Boolean }`.

- [ ] **Step 1: Testes que falham:** desligado → `false`; sem última fonte → `false`; já tocando → `false`; Android Auto (`com.google.android.projection.sink`) ou CarPlay (`com.autochips.carplayapp`) na frente → `false`; ligado + Jancar + nada tocando → `true`; `decode(encode(Session("com.spotify.music")))` ida e volta; `decode("lixo") == null`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar: 4 s após o gatilho, `JancarMediaClient` play ou `MediaController.transportControls.play()` da sessão do pacote (precisa do acesso a notificações; sem ele, não faz nada). Seção em Ajustes com o liga/desliga. **Step 4:** PASS. **Step 5:** Commit `feat(midia): retomar a música ao ligar a central`.

## Fase 8 — Cor do Fox

### Task 17: Máscara da lataria

**Files:**
- Create: `tools/car-photo/mascara.ps1`, `app/src/main/assets/car/body_mask.png`
- Modify: `tools/car-photo/LEIA-ME.md`

- [ ] **Step 1:** Script lê `app/src/main/assets/car/photo.png` e grava `body_mask.png` do mesmo tamanho (alfa 0..255): pixel de lataria = alfa da foto > 0, saturação < 0,18 e luminância entre 0,25 e 0,97; depois tira os polígonos (coordenadas da `photo.png`, medidos com `grade.ps1`) de: vidros, pneus e rodas, faróis, farol de milha, lanternas/setas, grade e entradas de ar, placa; suaviza a borda com desfoque de 1–2 px. Parâmetro `-preview <png>` = carro tingido de vermelho sobre xadrez.
- [ ] **Step 2:** Rodar `powershell -NoProfile -ExecutionPolicy Bypass -File tools/car-photo/mascara.ps1 -preview $env:TEMP/mascara.png`, abrir a prévia e conferir: vidros, pneus, faróis e placa sem vermelho; lataria (portas, capô, para-lamas, para-choque pintado, retrovisor) inteira vermelha, sem buracos. Ajustar polígonos até passar.
- [ ] **Step 3:** LEIA-ME: seção "Máscara da lataria" (quando regenerar: sempre que a foto mudar).
- [ ] **Step 4:** Commit `feat(carro): máscara da lataria da foto do Fox`.

### Task 18: Cores prontas e escolha na tela Fox

**Files:**
- Create: `ui/car3d/CarPaint.kt`
- Modify: `ui/car3d/CarPhoto.kt` (`PhotoLoad.Ready(image, body: ImageBitmap?)`; `CarPhotoView(..., paint: CarPaint = CarPaint.Original)`), `ui/car3d/CarScreen.kt` (toque na foto → fileira de cores; some após 6 s), `settings/SettingsStore.kt` (`carPaint`), debug `CarPreviewActivity` (`--es paint Vermelho`)
- Test: `ui/car3d/CarPaintTest.kt`

**Interfaces:**
- Consumes: `assets/car/body_mask.png` (Task 17).
- Produces: `enum class CarPaint(val label: String, val swatch: Long?, val gain: Float, val lift: Float) { Original, Vermelho, Preto, Branco, Grafite, AzulMarinho, AzulClaro, Amarelo, VerdeLimao, Laranja; companion fun fromName(n: String?): CarPaint }`; `fun CarPaint.matrix(): FloatArray?` (20 valores de `ColorMatrix`; `Original` = `null`); `fun paintAvailable(body: ImageBitmap?): Boolean`.

Matriz: luminância L = 0,299R + 0,587G + 0,114B; cada canal de saída c = L × cor_c × `gain` + `lift` × cor_c × 255 (linhas com os pesos de L multiplicados por `cor_c × gain`, coluna 5 = `lift × cor_c × 255`; alfa intacto).

- [ ] **Step 1: Testes que falham:** `Original.matrix() == null`; aplicar a matriz a um cinza claro (200,200,200): Vermelho dá R > 1,8×G e R > 1,8×B; Preto dá os três canais < 90; Branco dá os três > 200; as 9 cores dão resultados diferentes entre si; `fromName("x") == Original`; `paintAvailable(null) == false` (Review Focus 5).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar; `rememberCarPhoto` monta a camada da lataria uma vez (cópia ARGB da foto + máscara com `PorterDuff.Mode.DST_IN`), fora da thread da tela; sem máscara → `body = null` e a fileira não aparece. Desenho: foto → camada com `ColorFilter.colorMatrix` → brilhos e balões como hoje. Ajustar `gain`/`lift` de cada cor olhando a prévia.
- [ ] **Step 4:** PASS; prévias `CarPreviewActivity --es paint Preto|Branco|Vermelho --ez lights true` conferidas no emulador. **Step 5:** Commit `feat(carro): trocar a cor do Fox por diversão`.

### Task 19: Documentação e versão

**Files:**
- Modify: `app/CLAUDE.md` (decisões novas: Ajustes, atualização, bateria/água como aviso, scanner só leitura, viagens por ligada, custo, limite, cor; o que falta testar no carro), `docs/INSTALL.md` (já tocado na Task 4), `app/build.gradle.kts` (`versionCode = 16`, `versionName = "1.8.0"`)

- [ ] **Step 1:** Atualizar a documentação e a versão. `./gradlew :app:testDebugUnitTest` → todos PASS; `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 2:** Commit `chore: versao 1.8.0 (versionCode 16)`.
