# Fase 6 — Boot e instalação como HOME (spec)

Mestre: §6.6, §6.7, §3.1. Depende das Fases 1–4.

## Objetivo
O launcher abre sozinho ao ligar, pode ser definido como HOME na central sem root, e **sempre** dá para voltar ao launcher de fábrica.

## Requisitos
1. **`BootReceiver`** (`BOOT_COMPLETED`, `exported=true`, só essa ação) que inicia a `MainActivity` com `FLAG_ACTIVITY_NEW_TASK`. Na API 27 não há restrição de iniciar Activity em segundo plano; **não** usar `fullScreenIntent`/overlay. Falha engolida e registrada em log.
2. **Definição como HOME:** `adb shell cmd package set-home-activity com.rocha.jancarlauncher/.MainActivity` (único caminho; sem seletor na central). O comando só roda **depois** do rollback verificado.
3. **`docs/INSTALL.md`** (pt-BR) com: pré-requisitos (ADB Wi-Fi `<IP_DA_CENTRAL>:5555`, reconectar quando cair), `adb install -r`, `set-home-activity`, **rollback** (`... com.android.launcher3/com.jancar.launcher.act.ActLauncher`), `WRITE_SETTINGS` (Fase 4), lembrete de **desligar a depuração sem fio** depois (porta 5555 sem senha, ficha técnica), como desinstalar/atualizar, e o que fazer se a tela ficar sem launcher (recuperação: ADB USB ou o comando de rollback; botão físico de Home).
4. **Resiliência da HOME:** sem exceção não tratada nos caminhos de inicialização; `Application.onCreate` leve; `Thread.setDefaultUncaughtExceptionHandler` que registra e **reinicia a MainActivity** com limite (no máx. 3 reinícios em 60 s, para não entrar em laço).
5. **Botão Home do sistema** volta à Home em qualquer tela do app, inclusive depois de abrir outro app (singleTask + `onNewIntent` → `screen = Home`).
6. **Atualização com HOME ativo:** `adb install -r` mantém a definição de HOME; documentar teste.
7. **Sem permissões extras** além de `RECEIVE_BOOT_COMPLETED`, `BLUETOOTH`, `WRITE_SETTINGS` (opcional).

## Fora de escopo
Qualquer modificação de sistema além do `set-home-activity`; root; desativar apps (já feito pelo usuário).

## Decisões minhas
- Handler de crash com reinício limitado (3 em 60 s).
- Registrar logs só em `Log.w/e` (sem arquivos em disco, para poupar `/data` e I/O).

## Aceite (no aparelho real)
1. Rollback testado: define-se outro launcher e volta ao de fábrica.
2. Após `set-home-activity`, reiniciar a central → abre a Boas-vindas (se `BOOT_COUNT` mudou) → Home, sem tela intermediária do launcher de fábrica por mais que o tempo normal de boot.
3. Botão Home volta à Home de dentro de Rádio/Música/Android Auto.
4. Matar o processo (`adb shell am force-stop`) → o sistema reabre a HOME.
5. `adb install -r` de nova versão mantém tudo funcionando.
6. Simular crash (build debug com ação oculta): reabre a Home; após 4 crashes em 60 s não entra em laço.
