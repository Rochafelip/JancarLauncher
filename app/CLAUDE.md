# JancarLauncher — contexto do projeto

Launcher Android minimalista para a central automotiva **Jancar 8227L**, substituindo o launcher de fábrica (que não tem root e não permite troca de launcher pelas Configurações normais). Objetivo: abrir direto nas funções usadas no dia a dia — Android Auto/CarPlay, Rádio, Música, Vídeo — mais um conjunto pequeno de atalhos de sistema, tudo num visual minimalista inspirado na lataria prata do carro do usuário (um VW Fox) e nos faróis de LED (acento azul-aço frio).

Referências vivas (abrir antes de codar):
- **Preview visual (Design canvas, 3 telas: Boas-vindas / Principal / Gaveta de apps):** https://claude.ai/artifact/Ssx64idLCv62PSoBGhCcdE
- **Documento de mapeamento técnico da central (pacotes, activities, inventário, restrições):** https://claude.ai/artifact/2woDoZfroQzoJRvmj9XGz2

## Hardware / software alvo (não emular suposições, usar exatamente isto)

| Campo | Valor |
| --- | --- |
| Central | Jancar 8227L (MediaTek/Autochips AC8227L, ARMv7 32 bits) |
| Android real | 8.1 (API 27) — **minSdk = 27** |
| RAM | 2 GB — app precisa ser leve |
| Tela | 1024×600, densidade 160 |
| Root | Não tem. `ro.secure=1`, build `user`. Nenhuma operação pode depender de root. |
| Troca de launcher | Não existe seletor nas Configurações da central. Só via `adb shell cmd package set-home-activity`. |
| Wi-Fi | Só 2,4 GHz. ADB sem fio em `<IP_DA_CENTRAL>:5555`, sem senha, **instável** (cai com frequência). |

## Stack definida

- **Kotlin + Jetpack Compose** (não XML/Views) — o padrão reativo do Compose casa com os estados da UI (tema, brilho, now playing).
- `minSdk 27`, `targetSdk` pode ser mais novo, mas todo comportamento precisa funcionar em API 27.
- Build debug é suficiente pra instalar (`adb install`), a central não valida assinatura de loja.

## Telas (ver o preview visual pra referência exata de layout/cores)

1. **Boot/Boas-vindas** — aparece ao ligar o carro: foto do carro do usuário (tratada: desfoque + escurecida + duotone azul-acinzentado) como fundo, relógio centralizado, "toque para continuar". Leva pra tela Principal.
2. **Principal (Home)** — launcher substituto, abre sozinho no boot:
   - Relógio grande + data, canto superior esquerdo
   - Ícones no canto superior direito: alternar tema claro/escuro, ajustar brilho (abre painel com slider), abrir gaveta de apps
   - ~~Card "Tocando agora"~~ — **removido** (decisão do usuário: sem acesso a notificações, o mais limpo possível)
   - Fileira de chips minimalistas: Wi-Fi, Bluetooth, GPS, Câmera de ré (manual), Ajustes — cada um abre a tela/app correspondente direto
   - 4 blocos grandes (ação principal): **Auto** (Android Auto/CarPlay), **Música**, **Rádio**, **Vídeo**
3. **Gaveta de apps** — grid com os apps secundários (os que não estão na Home), cada um com ícone, nome e tag "App" (instalado por fora) ou "Sistema": VLC, NewPipe, F-Droid, Lightning Browser, Galeria, Arquivos, Bluetooth, Música BT, EQ, Informações do veículo, Scrcpy for Android, Android OBD-II Reader.

## Paleta de cores (tokens, usar exatamente estes hex)

**Escuro (padrão):** bg `#0a0b0f` · fg `#eef0f2` · muted `#888d94` · faint `#53565c` · surface `#15161b` · surface-hover `#1c1e24` · icon `#c9cdd3`

**Claro:** bg `#f1f2f4` · fg `#16181c` · muted `#696d73` · faint `#9a9da3` · surface `#fbfbfc` · surface-hover `#e9eaed` · icon `#3b3e44`

**Acento:** `#6fa3d8` (azul-aço frio, inspirado no farol de LED do carro do usuário)

Tema alterna por toggle manual (ícone sol/lua) — persistir a escolha do usuário entre sessões (`SharedPreferences` ou `DataStore`).

## Intents confirmados (os 4 botões principais)

| Botão | Pacote | Activity | Status |
| --- | --- | --- | --- |
| Android Auto | `com.google.android.projection.sink` | `.ui.AndroidAutoActivity` | Confirmado |
| CarPlay (fallback) | `com.autochips.carplayapp` | `.CarPlayActivity` | Confirmado — **nunca** usar `com.autochips.carplay` (sem "app"), é só serviço de fundo |
| Rádio | `com.jancar.radio` | `.activity.RadioMainActivity` | Confirmado |
| Música | `com.jancar.music` | **não resolvida** — tem mais de uma activity `LAUNCHER`, `resolve-activity` cai no `ResolverActivity` | **BLOQUEANTE**, ver Pendências |
| Vídeo | não confirmado | não confirmado | **BLOQUEANTE**, ver Pendências |

```kotlin
// Android Auto, com fallback pra CarPlay — mas ver "Decisão: Auto vs CarPlay" abaixo,
// a escolha deve ser feita ANTES do startActivity, não por try/catch depois
Intent().setClassName(
    "com.google.android.projection.sink",
    "com.google.android.projection.sink.ui.AndroidAutoActivity"
)

// Rádio
Intent().setClassName("com.jancar.radio", "com.jancar.radio.activity.RadioMainActivity")

// Música — provisório até confirmar a activity exata (risco: pode abrir diálogo de escolha)
Intent(Intent.ACTION_MAIN).setPackage("com.jancar.music").addCategory(Intent.CATEGORY_LAUNCHER)
```

Todo `startActivity` dos botões precisa de `try { } catch (e: ActivityNotFoundException) { }` — se o pacote-alvo foi desativado, o launcher não pode travar.

## Decisões de design já tomadas (não reabrir essas discussões)

- **Launcher-substituto**, não atalho: abre sozinho no boot via `BroadcastReceiver` escutando `BOOT_COMPLETED` (permissão `RECEIVE_BOOT_COMPLETED`, suportada na API 27) e é definido como `HOME` via `set-home-activity`.
- **Detecção Android Auto vs. CarPlay: por Bluetooth + cabo**, decidida *antes* de chamar `startActivity` (checar `BluetoothAdapter.getBondedDevices()` / tipo de dispositivo pareado, e estado de conexão USB), nunca por tentativa-e-erro depois de abrir.
- **Sem "Tocando agora" e sem `NotificationListenerService`** (decisão do usuário, 2026-11-05): o app não pede nenhuma permissão especial; os controles de mídia ficam nos apps nativos.
- **Boas-vindas** aparece após cada reinício (`BOOT_COUNT`) e ao acordar da suspensão com a tela apagada por ≥ 10 min (a central suspende ao desligar o carro, não reinicia).
- **Música e Vídeo**: activity **fixa** em `AppTargets` (sem descoberta automática); enquanto não preenchidas mostram o aviso "não configurado".
- Botão de **Câmera de ré manual** — abre `com.autochips.backcarapp` sob demanda, fora da marcha-ré (não confirmado se o app aceita isso, ver Pendências).
- Toque mínimo de **44–48px** em todo elemento interativo (ajustado numa revisão de UX; os chips e os controles do player estavam menores que isso na primeira versão). Os **chips de atalho** da Home têm **60dp** de altura (`Dimens.ChipHeight`, alvo 56–64dp pra uso dirigindo; verificado no emulador 1024×600).

## Pendências bloqueantes

Nenhuma — Música, Vídeo, Galeria, Arquivos, Música BT, EQ, Veículo, Scrcpy e OBD-II todos confirmados via `dumpsys` na central e configurados em `AppTargets.kt`/`DrawerCatalog.kt` (ver commit "fix: configura pacotes faltantes...").

## Pendências não-bloqueantes

- Confirmar se `com.autochips.backcarapp` aceita abertura manual (fora da marcha-ré).
- Confirmar se dá pra desativar a depuração ADB sem fio pelas Configurações da própria central.
- **Calibrar `can/CanBusClient.kt` na central real** (ver seção "Integração CAN bus" abaixo) — a ordem dos campos do Parcel ainda não foi validada com dados reais.

## Comando de rollback (guardar antes de testar `set-home-activity`)

```
adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher
```

## Estado da implementação (atualizado em 2026-11-05)

Código pronto e testado em emulador (Android 15, 1024×600 @160 dpi); **nada testado ainda na central real** (API 27).
- Specs, planos e registro de decisões: `docs/superpowers/` (`ledger.md` tem as decisões e o que foi/não foi verificado).
- Instalar na central: `docs/INSTALL.md`. Rollback: `adb shell cmd package set-home-activity com.android.launcher3/com.jancar.launcher.act.ActLauncher`.
- `compileSdk 37`, `minSdk 27`, `targetSdk 36`; Compose `foundation`+`ui` (sem Material3); DataStore para tema/brilho.
- Todo `startActivity` do app passa por `launch/AppLauncher` (apps), `launch/SystemSettingsOpener` (Settings), `boot/CrashRestarter` e `power/ScreenStateTracker` (reabrir a própria Home).
- Pacotes confirmados na tabela (`launch/AppTargets.kt`): Android Auto, CarPlay (`carplayapp`), Rádio, Música, Música BT, Vídeo, Galeria, Arquivos, EQ, Veículo, Scrcpy, OBD-II. Pelo nome do pacote: VLC, NewPipe, F-Droid, Lightning (a confirmar na central).
- Fontes em `ui/theme/Type.kt` revisadas (nov/2026) pra distância de leitura automotiva (ISO 15008, ~50–70cm).

## Integração CAN bus (`can/`, em calibração — não confiar nos valores ainda)

O ContentProvider `content://com.autoai.canbus.provider` **não tem dados ao vivo** (é só banco de
protocolos/marcas) — descoberta corrigindo a pendência anterior. Os dados reais vêm de um serviço
exportado sem permissão, `com.can.activity/com.can.ui.CanPopWind` (action
`autoai.intent.action.CANBUS_SERVICE`), acessado via `Binder.transact` direto (engenharia reversa
do bytecode, sem AIDL gerado — ver comentários em `can/CanBusClient.kt`).

- `CanBusClient`: instância única em `JancarApp.canBus` (nunca criar uma por tela).
- `VehicleInfoCard`: card de debug, hoje chamado temporariamente em `HomeScreen.kt` — **remover
  depois de calibrar**, ou mover pra trás de um gesto de debug.
- **A ordem dos campos no Parcel foi inferida do bytecode, não validada com o carro ligado.**
  Calibrar com `adb logcat -s CanBusClient`: se `restante` (bytes sobrando no Parcel) for `0`, a
  ordem bate; abrir/fechar portas e ligar setas pra conferir se os números mudam como esperado.
- Usa `kotlinx-coroutines-core` como dependência principal (antes só existia em `testImplementation`).
