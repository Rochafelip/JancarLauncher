# Fase 3 — Lançamento de apps (núcleo lógico) (spec)

Mestre: §2 (anti-objetivos), §6.1, §6.2. Depende da Fase 1 (pode andar em paralelo à 2).

## Objetivo
Lógica testável que decide e executa o lançamento de cada alvo sem nunca derrubar o launcher e sem cair no seletor de apps.

## Requisitos
1. **Tabela única `AppTargets`**: cada alvo tem `id`, `ComponentName?` (pacote+activity) ou apenas `packageName` (fallback via `getLaunchIntentForPackage`), `displayName`, `kind` (App/Sistema). Alvos: `ANDROID_AUTO`, `CARPLAY`, `RADIO`, `MUSIC`, `VIDEO`, `BACK_CAMERA`, e os 12 da gaveta (Fase 4 completa a lista). Valores confirmados do `CLAUDE.md`; `MUSIC`/`VIDEO` com **activity fixa informada pelo usuário** (nula = "não configurado").
2. **`AppLauncher.launch(target): LaunchResult`** com `LaunchResult = Started | NotInstalled | Disabled | Failed(Throwable)`. Nunca lança exceção para fora. Passos: checar existência+habilitação via `PackageManager` **antes**; criar `Intent` explícito (`setClassName`/`setComponent`), `FLAG_ACTIVITY_NEW_TASK`; `try/catch` de `ActivityNotFoundException`, `SecurityException` e `RuntimeException`.
3. **Sem `ResolverActivity`:** intents sempre explícitos; no fallback de pacote, usar `getLaunchIntentForPackage`; se retornar nulo → `NotInstalled`. Música e Vídeo usam **activity fixa** da tabela (sem `queryIntentActivities` nem fallback por pacote); se nula → `NotInstalled` com toast "não configurado".
4. **`ProjectionChooser.choose(input): Projection`** (função pura). Entrada: lista de `BondedPhone(name, majorDeviceClass)`, `usbPhoneAttached: Boolean`, `userOverride: Projection?`. Regra (spec mestre §6.2, minha escolha): override manda; senão, qualquer pareado com nome contendo `iphone`/`ipad`/`apple` (case-insensitive) → `CarPlay`; senão `AndroidAuto`. `usbPhoneAttached` só desempata: sem celular pareado mas com USB ligado → `AndroidAuto`. Alvo `Auto` resolve para `ANDROID_AUTO`/`CARPLAY` **antes** de `launch`.
5. **Leitura do ambiente (borda fina, não testada em JVM):** `BluetoothAdapter.bondedDevices` filtrando `BluetoothClass.Device.Major.PHONE`; `UsbManager.deviceList.isNotEmpty()`. Se faltar permissão/adaptador, entradas vazias — nunca exceção.
6. Mensagem ao usuário (toast curto, pt-BR) para `NotInstalled`/`Disabled`/`Failed`; **nada de crash**.

## Fora de escopo
UI dos blocos (Fase 4); persistência de override (a regra existe, o controle de override fica para depois).

## Aceite
- Testes unitários: `AppLauncherTest` (instalado→`Started`; ausente→`NotInstalled`; desativado→`Disabled`; starter lançando exceção→`Failed`), `ProjectionChooserTest` (iPhone→CarPlay; Android→AA; nenhum+USB→AA; nenhum→AA; override).
- No aparelho: botão de teste abre o Rádio (`com.jancar.radio/.activity.RadioMainActivity`) e o Android Auto; abrir alvo inexistente só mostra aviso.
- Nenhum `startActivity` do projeto fora de `AppLauncher` (checar com grep).
