# Fase 14 — Plano: botões do volante param com o Android Auto aberto

Sem spec separada: o diagnóstico está abaixo. **A causa raiz ainda não foi confirmada** — a Fase A
(evidência no carro) vem antes de qualquer código.

## Sintoma (relato do usuário, 2026-11-10)

- Música da Jancar (pendrive/USB) **ou** Rádio tocando, entra no **Android Auto** → **todos** os
  botões do volante param, **inclusive volume**.
- Com o launcher original da Jancar funcionavam.

## O que o código descompilado mostra (`../jancar-apks/ivi-services/jadx`)

1. **Toda tecla do volante passa por uma única thread.** `IVICore.onKeyEvent(IVIKey.Key)` é
   `@Subscribe(threadMode = MAIN)` no processo `com.jancar.services` e chama
   `KeyCodeUtil.onKeyEvent` — volume (`AudioCtrl.incVolume`), próxima/anterior
   (`handleNext`/`handlePrev` → `MediaCtrl.next()` → `MediaService.onMediaControl`, também `MAIN`)
   e play/pause. Thread principal presa = todas as teclas mortas, volume incluso.
2. **A mesma thread chama o launcher e espera.** `MediaService.onMediaInfoChanged` /
   `onMediaStateChanged` (`MAIN`) percorrem `getDeviceNodes()` e chamam `onMediaChange` /
   `onPlayStateChange` em cada `IMediaInfoCallback` registrado — o nosso `JancarMediaClient` é um
   deles. O proxy usa `transact(..., 0)` (síncrono, não oneway). Com música tocando o estado chega
   **a cada segundo**; a troca de faixa leva a capa em pixels ARGB crus. O `CarService` faz o mesmo
   com os `ICarCallback` (o `JancarCarClient` registra um).
3. **O launcher continua registrado em segundo plano.** As conexões vivem num `DisposableEffect` do
   `AppRoot`, que só é desfeito quando a activity é destruída — não no `onStop`. Ao abrir o AA o
   launcher fica parado, em segundo plano (pouca CPU), numa central de 2 GB com o AA decodificando
   vídeo. Cada aviso pode deixar a thread da central esperando o launcher; as teclas entram na fila.
4. O launcher original não registrava esse callback.

Descartado: o AA que a central coordena (`IVISystem.PACKAGE_ANDROID_AUTO`) é o mesmo pacote que
abrimos (`com.google.android.projection.sink`); os códigos `TRANSACTION_*` de `IMedia`/`ICar`
conferem com o descompilado.

**Hipótese:** o `IMediaInfoCallback` (e talvez o `ICarCallback`) registrado pelo launcher enquanto o
AA está na frente prende a thread principal de `com.jancar.services`, que é a mesma que processa as
teclas do volante.

## Fase A — Evidência no carro (sem mudar código)

Cenário de reprodução: `adb connect 192.168.3.33:5555`, música do pendrive tocando, abrir o AA pelo
launcher, confirmar que os botões falham.

### T14.1 Teste decisivo: matar o launcher
Com os botões falhando: `adb shell am force-stop com.rocha.jancarlauncher`. Os callbacks morrem
(`linkToDeath` → `removeDeviceNode`).
- Botões voltam na hora → culpa do launcher, pelas conexões (a UI nem está visível). Seguir T14.2.
- Não voltam → hipótese errada; voltar ao diagnóstico (não seguir para a Fase B).

### T14.2 Log da central
`adb logcat -v threadtime -s JLOG:V JancarMediaClient:V JancarCarClient:V` e apertar volume +/−
no volante. Procurar:
- a linha de `IVICore.onKeyEvent` / `KeyCodeUtil.onKeyEvent` (nome da tecla + "type"): ausente ou
  atrasada e chegando em rajada = thread presa;
- motivos de bloqueio que a central loga: "CCD is on", "In fast reverse", "Volume Step is Start",
  "Bluetooth calling, key … not allow".

### T14.3 Comparação com o launcher original
Rollback (`adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher`),
mesmo cenário. Confirmar que os botões funcionam com o AA. Voltar para o nosso launcher depois.

### T14.4 Isolar mídia × carro (se T14.1 confirmar)
Build de depuração com o `ICar` desligado (sem `carService.connect()` no `AppRoot`) e repetir; depois
o inverso (sem `jancarMedia.connect()`). Anotar qual das duas conexões causa a falha (ou ambas).

Registrar o resultado de T14.1–T14.4 no `ledger.md` antes de ir para a Fase B.

## Fase B — Correção (só com a hipótese confirmada; uma mudança por vez)

### T14.5 Regra de registro por ciclo de vida (teste primeiro)
`media/MediaCallbackPolicy.kt` (puro, testável em JVM): dado o estado do ciclo de vida da activity,
diz se o callback do `IMedia` deve estar registrado (só em `STARTED`/`RESUMED`).
`MediaCallbackPolicyTest`: `CREATED`→false, `STARTED`→true, `RESUMED`→true, `DESTROYED`→false.

### T14.6 `JancarMediaClient`: soltar/religar o callback sem desfazer o bind
Novos `fun detachCallback()` (envia `T_UNREGISTER_INFO_CALLBACK`) e `fun attachCallback()` (envia
`T_REGISTER_INFO_CALLBACK` + `T_REQUEST_INFO_AND_STATE`, para o card voltar atualizado). O
`onServiceConnected` só registra se estiver em primeiro plano (flag `@Volatile` controlada pelos
dois métodos). Continuam valendo as regras do cliente: nada de `setMediaInfo`/`setMediaState`/zonas.

### T14.7 `AppRoot`: ligar ao ciclo de vida
`LifecycleEventObserver` no `LocalLifecycleOwner`: `ON_START` → `attachCallback()`, `ON_STOP` →
`detachCallback()`. O `DisposableEffect` atual continua cuidando de `connect()`/`disconnect()`.
O card "Tocando agora" não aparece em segundo plano, então nada visível se perde.

### T14.8 (só se T14.4 apontar o `ICar`) Mesmo tratamento no `JancarCarClient`
Em segundo plano: desregistrar `ICarCallback` e `registerRealTimeInfo` (velocidade/RPM); manter só a
consulta periódica (`refreshSnapshot`, que roda nas threads binder do `CarService`, não na
principal) para o farol continuar comandando o modo noturno. Em primeiro plano: registrar de novo.

## Fase C — Validação no carro

### T14.9 Cenário original
AA + pendrive e AA + rádio: volume +/−, próxima/anterior, play/pause no volante — todos respondem.

### T14.10 Sem regressão
- Voltar para a Home: "Tocando agora" mostra a faixa/estado atuais logo ao aparecer.
- Modo noturno pelo farol continua funcionando com o launcher em segundo plano (acender o farol com
  o AA aberto, voltar à Home).
- Painel (CAN) inalterado.

### T14.11 Registro
Atualizar `ledger.md` (causa confirmada + evidências) e o `CLAUDE.md` (regra: callbacks síncronos
da Jancar só registrados com o launcher em primeiro plano).
