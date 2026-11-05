# Fase 11 — Plano

Spec: `../specs/2026-11-05-phase-11-indicador-adb-design.md`. Pacote `system/`.

## T11.1 Modelo (teste primeiro)
`system/AdbStatus.kt`:
```kotlin
sealed class AdbStatus {
    object Disabled : AdbStatus()
    object Enabled : AdbStatus()
    data class EnabledConnected(val remoteIp: String?) : AdbStatus()
}
```

## T11.2 Parser de `/proc/net/tcp` (teste primeiro)
`system/ProcNetTcpParser.kt`: `fun findEstablishedRemoteIp(lines: List<String>, portHex: String = "15B3"): String?` — recebe as linhas já lidas (não lê o arquivo, pra ser testável em JVM), procura `st == 01` (ESTABLISHED) na porta local 5555, devolve o IP remoto decodificado (hex little-endian → dotted). `ProcNetTcpParserTest`: linha de exemplo real do `/proc/net/tcp` com e sem conexão na 5555, arquivo vazio, linha malformada (não lança exceção, ignora a linha).

## T11.3 Leitura do sistema
`system/AdbStatusReader.kt`: `fun read(context): AdbStatus` — `Settings.Global.getInt(ADB_ENABLED)` decide `Disabled` vs `Enabled`; se `Enabled`, tenta ler `/proc/net/tcp` (`File("/proc/net/tcp").readLines()`) dentro de `runCatching`, repassa pro parser (T11.2); falha de leitura → `Enabled` sem IP (não é erro, é o caminho esperado se a central não permitir).

## T11.4 UI
`ui/home/AdbIndicator.kt`: ícone pequeno perto do relógio, visível só se `Enabled`/`EnabledConnected`; cor `muted`; toque → `SystemSettingsOpener`. Atualiza no mesmo tick de minuto das Fases 8/9 (reaproveitar, não duplicar).

## T11.5 Verificação no emulador
`adb tcpip 5555` + `adb connect 127.0.0.1:5555` do host; conferir se o indicador muda para conectado. Documentar se `/proc/net/tcp` é legível no AVD (geralmente sim, pode ser diferente na central real — registrar os dois resultados).

## T11.6 Commit
`feat: indicador de ADB sem fio ativo/conectado`.
