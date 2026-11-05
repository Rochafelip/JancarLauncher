# Fase 11 — Indicador de ADB sem fio (spec)

## Objetivo
Lembrete visual discreto na Home de que o ADB sem fio está ativo (a porta fica aberta sem senha — ver `app/CLAUDE.md`).

## Requisitos
1. **`AdbStatus`** (`system/`), lógica pura/testável: recebe o valor de `Settings.Global.ADB_ENABLED` e devolve `Disabled | Enabled | EnabledConnected(remoteIp: String?)`.
2. **Leitura do estado ativo**: `Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)` — deve funcionar como app normal.
3. **Leitura de conexão ativa**: tentar ler `/proc/net/tcp` procurando uma entrada ESTABLISHED na porta 5555 (hex `15B3`), extraindo o IP remoto (também em hex, formato little-endian). **Melhor esforço**: se o arquivo não puder ser lido (`SecurityException`/`IOException`), cair para `Enabled` sem o IP, sem travar nem logar como erro.
4. **UI**: ícone pequeno perto do relógio/chips, só visível quando `Enabled`/`EnabledConnected`. Toque abre a tela de depuração via `SystemSettingsOpener`, se existir na central.
5. **Checagem**: a cada tick de minuto (mesmo mecanismo das Fases 8/9), não precisa de polling mais frequente.

## Decisões minhas
- Sem o IP (`/proc/net/tcp` ilegível): mostra só "ADB ativo", sem tentar outras formas de descobrir quem conectou (ex. `netstat` via `Runtime.exec`, que tende a ser bloqueado do mesmo jeito).
- Cor do indicador: usa o token `muted`, sem alarmar — é um lembrete, não um alerta de segurança urgente.

## Fora de escopo
Desligar o ADB pelo próprio launcher.

## Aceite
1. `AdbStatusTest`: parsing de `/proc/net/tcp` (entradas de exemplo, com e sem conexão na porta 5555; arquivo vazio/malformado não derruba o parser).
2. No emulador: `adb tcpip 5555` + `adb connect` muda o indicador para "conectado" (se `/proc/net/tcp` for legível no emulador; documentar o resultado).
3. **Na central real**: confirmar se `/proc/net/tcp` é legível pelo app; se não for, o indicador cai para "ADB ativo" sem IP — isso é aceitável e deve ficar registrado no ledger, não é uma falha a corrigir.
