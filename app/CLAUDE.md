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
   - Ícones no canto superior direito: alternar tema claro/escuro, ajustar brilho (abre painel com slider), abrir gaveta de apps, **Painel** (velocímetro), **apagar tela** (ícone de power) — botões de **60dp** com ícone de 24dp (`Dimens.TopButton`/`TopButtonIcon`; pedido do usuário, 2026-11-07: "ainda acho pequenos")
   - Card **"Tocando agora"** — **reativado** (2026-11-07, ver decisão abaixo): faixa, capa e transporte da Música da Jancar (IMedia) ou de uma sessão de mídia padrão
   - Fileira de chips minimalistas: Wi-Fi, Bluetooth, GPS, Ajustes — cada um abre a tela/app correspondente direto (o chip de Câmera de ré foi **removido** em 2026-11-07 a pedido do usuário: "não serve para nada")
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
- **"Tocando agora" reativado** (decisão do usuário, 2026-11-07 — **substitui** a de 2026-11-05 "sem Tocando agora e sem NotificationListenerService"). O app de Música da Jancar **não usa MediaSession padrão**: publica a faixa no serviço próprio `com.jancar.services/.media.MediaService` (`IMedia`), lido por `media/JancarMediaClient` (registro de `IMediaInfoCallback` + transporte play/pause/próxima/anterior; códigos dos campos `TRANSACTION_*` do `ivi-services.vdex`, não inferidos; nunca `setMediaInfo`/`setMediaState`/zonas/arquivos). Calibrado na central: título = nome do arquivo sem tag (o card tira a extensão), capa = pixels ARGB crus `num1×num2×4` convertidos para PNG, estado 3 = tocando. Regra de fonte em `media/JancarNowPlaying` (quem toca ganha, Jancar primeiro). A sessão padrão (Bluetooth/apps de terceiros) ainda exige **acesso a notificações**, concedido na central via `adb shell cmd notification allow_listener com.rocha.jancarlauncher/com.rocha.jancarlauncher.media.NowPlayingListenerService` — a sessão do Bluetooth da central veio sem título, então pode não ajudar. O estado da Jancar chega a cada segundo (posição): o `AppRoot` só recompõe quando muda tocando/parado.
- **Modo noturno pelo farol** (decisão do usuário, 2026-11-07, 1.5.1): farol aceso = tema escuro + brilho noturno; apagado = volta o tema/brilho de antes (`settings/NightModePolicy`, `NightModeController`, avaliado no `AppRoot` com 3 s de espera). Sem leitura do farol (ICar desconectado, emulador) vale a janela fixa 20:00–05:30, como antes.
- **Boas-vindas** aparece após cada reinício (`BOOT_COUNT`) e ao acordar da suspensão com a tela apagada por ≥ 10 min (a central suspende ao desligar o carro, não reinicia).
- **Música e Vídeo**: activity **fixa** em `AppTargets` (sem descoberta automática); enquanto não preenchidas mostram o aviso "não configurado".
- **Sem câmera de ré manual** (decisão do usuário, 2026-11-07): o chip que abria `com.autochips.backcarapp` fora da marcha-ré foi removido; a câmera continua abrindo sozinha ao engatar a ré (comportamento da central). Não recolocar.
- **Apagar tela** (decisão do usuário, 2026-11-07): botão na Home que deixa a tela preta e **acorda no toque** direto na Boas-vindas, **sem senha**. Sem root/Device Admin não dá pra desligar o display de verdade (o toque não acenderia), então é tela preta + brilho da janela em 0 + `FLAG_KEEP_SCREEN_ON` + barras escondidas (`power/ScreenOffPolicy`, `ui/screenoff/ScreenOffScreen`). O som continua; volume/mídia passam direto sem acordar; outras teclas acordam; só o brilho da **janela** muda (nunca o do sistema); modo noturno com a tela apagada só grava e aplica ao acordar; outro app por cima (ex.: câmera de ré) → ao voltar, Boas-vindas. A flag de manter acesa vai na janela (`addFlags`/`clearFlags`), **não** em `view.keepScreenOn` — esta ficava presa quando desfeita com a activity parada.
- **Painel** (pedido do usuário, 2026-11-07: "velocidade e RPM grandes, estilo FuelTech"; resto decidido por mim): tela `Screen.Dash` (`ui/dash`), **segue o tema da Home** (claro/escuro, inclusive o modo noturno pelo farol; pedido do usuário, 2026-11-13 — antes era sempre escura). No claro, âmbar/vermelho/azul/verde ficam mais escuros pra continuar legíveis no fundo branco (`DashPalette` em `DashScreen`); as faixas de aviso e a SHIFT mantêm as mesmas cores nos dois temas — barra de RPM segmentada (35 × 200 rpm) com **aviso de troca por modo** (pedido do usuário, 2026-11-10; ele troca por volta de 3000 rpm no dia a dia e 5000 andando bravo): botão no topo do Painel alterna **DIA A DIA** (troca 3000) ↔ **BRAVO** (troca 5000), o modo fica salvo (`SettingsStore.driveMode`). A 500 rpm do ponto (`SHIFT_APPROACH_RPM`) os segmentos acesos e o número do RPM ficam âmbar; no ponto, vermelhos piscando — barra, números da escala na faixa vermelha, número do RPM e faixa SHIFT piscam **juntos**, num pisca só (pedido do usuário, 2026-11-13); só no BRAVO entra a faixa amarela com "SHIFT" em vermelho, à frente de qualquer outro aviso (`DashModel.DriveMode`/`shiftStage`). Escolhido entre 3 modelos (o usuário preferiu "barra e número mudam de cor" a borda da tela acesa ou luzes de troca em cima), velocidade gigante, RPM e, embaixo, só ícones grandes (36dp) com combustível, temperatura externa e odômetro (sem rótulos). **Todos os sensores disponíveis** (pedido do usuário, 2026-11-10: "trazer todos os sensores"): no canto inferior direito, luzes-espia em verde (seta esq., farol, seta dir.; apagadas = quase invisíveis) e o carro visto de cima com portas/porta-malas/capô abertos em âmbar (`DoorsDiagram`). Portas = `Cabin` do CAN (testado: motorista = 1); setas = `Lights` do CAN (**testado no Fox em 2026-11-13**: chega a posição da alavanca, fixa enquanto ligada, então **o Painel pisca o indicador sozinho** (1 piscada/s, pedido do usuário "mais lento"; `TURN_BLINK_HALF_MS`, com 800 ms de folga caso a caixa mande a lâmpada piscando); pisca-alerta = as duas setas em 1, o campo `emergencyFlasher` nunca vem; só chega com o Painel aberto, que é quando o launcher está ligado ao app CAN); farol = fio do chicote (ICar; acende com a lanterna, não distingue farol baixo de alto — o app CAN lê esses bits da caixa mas não os repassa: o `LightInfo` só tem setas e pisca-alerta, sem fonte sem root). Ficam de fora por não terem fonte: bateria, água/óleo, cinto (o PQ não preenche: fica sempre no padrão 1), capô (o PQ não manda; o desenho aceita se vier), marcha (`gearShiftPosition` do PQ só vale com ré + freio de mão, e a câmera de ré já aparece). Prévia com dados falsos (só debug): `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.DashPreviewActivity [--ez moving true] [--ez light true]`. **O mais limpo possível** (pedido do usuário, 2026-11-07): sem bateria, sem autonomia, sem troca de óleo, sem "motor aquecendo"; o status de conexão só aparece se faltarem dados ("Sem dados do carro", após 4 s). **Avisos só quando acontecem** (`DashModel.alerts`, faixa colorida que alterna a cada 3 s): hoje só combustível na reserva (0 < L ≤ 7, âmbar). O aviso de freio de mão andando foi **removido na 1.5.1**: o fio do freio de mão não está ligado nesta instalação (dois testes, nunca mudou). Porta aberta só vira aviso (**vermelho**, primeiro da lista) **andando** a ≥ 5 km/h; parada é normal e o app CAN da central já mostra popup. Temperatura do motor/óleo não existe nesta central (ver seção CAN). O Painel mantém a conexão única do CAN (a do ICar é do `AppRoot`); o diagnóstico usa `CarStateDetails`/`VehicleInfoDetails` (sem conexão própria). Layout compactado para a área útil da central (~534dp: a barra de status da Jancar ocupa o topo). Faixas em `DashModel` (pensadas pro Fox, ajustar lá). Valor desconhecido = "—" em cinza. Inspiração de estilo apenas — sem logo/identidade da FuelTech.
- **Carro 3D** (pedido do usuário, 2026-11-13: "tela 3D interativa do meu Fox G2 2014", misturada ao visual do projeto): tela `Screen.Car` (`ui/car3d`), aberta pelo Painel (botão de carro no topo ou toque no desenho das portas); voltar → Painel. Mesma moldura, ícones e tema do Painel; conta-giros e velocímetro + combustível no estilo do painel original do Fox G2 (foto `docs/Fox-photos/img.png`) dos lados, com o 3D ocupando a largura toda por trás deles (carro grande); embaixo luzes-espia, portas por extenso, temperatura externa e hodômetro. Pintura com verniz e reflexo de estúdio; luzes acendem/apagam com fade de lâmpada (`CarRig.fadeLamp`); repetidor de seta no retrovisor (o Fox do dono tem). O carro gira 360° com o dedo, pinça aproxima, dois toques centraliza; abriu uma porta e ninguém mexeu há 8 s, a câmera vai até ela. Anima só o que tem fonte real: 4 portas + porta-malas (CAN `Cabin`), setas/pisca-alerta (CAN `Lights`, piscando junto com os ícones), faróis + lanternas (fio de iluminação, um estado só). **Sem** capô, farol alto, luz de freio, bateria, temperatura do motor (sem fonte; ver auditoria). **GPU da central = Mali-450, só OpenGL ES 2.0** (medido por adb): Filament/SceneView não rodam, então o renderizador é próprio (`CarRenderer`, ES 2.0, 4× MSAA, reflexo falso de céu/chão, desenha só quando algo muda) com leitor de `.glb` mínimo (`Glb`). Modelo em `assets/car/fox.glb`; regras para o modelo comprado (nomes dos nós, pivôs, orçamento) em `docs/car3d/contrato-do-modelo.md`. **Hoje é uma maquete provisória** gerada por `tools/car3d/make-placeholder-fox.mjs` (medidas reais do Fox G2; a tela mostra "MODELO PROVISÓRIO"); o usuário escolheu comprar um modelo de Fox G2 e ajustar às fotos (`docs/Fox-photos/`). Prévia com estados falsos (só debug): `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.CarPreviewActivity [--es open fl,fr,rl,rr,trunk,hood] [--ez lights true] [--es turn left|right|hazard] [--ez light true]`. Testado só no emulador; **falta medir FPS e memória na central**.
- **Restauração de tela** (`ui/nav/ScreenRestore`): ao recriar a activity, nunca volta no Painel/Gaveta/Carro (→ Home) nem na tela apagada (→ Boas-vindas). Motivo: no emulador o app abriu sozinho no Painel ~4 vezes em ~50 aberturas logo após reinstalar/forçar fechamento; **causa não confirmada** (o roteiro HOME + `am kill` não reproduz nem sem a regra). Se voltar a acontecer na central, investigar de novo.
- Toque mínimo de **44–48px** em todo elemento interativo (ajustado numa revisão de UX; os chips e os controles do player estavam menores que isso na primeira versão). Os **chips de atalho** da Home têm **60dp** de altura (`Dimens.ChipHeight`, alvo 56–64dp pra uso dirigindo; verificado no emulador 1024×600).

## Pendências bloqueantes

Nenhuma — Música, Vídeo, Galeria, Arquivos, Música BT, EQ, Veículo, Scrcpy e OBD-II todos confirmados via `dumpsys` na central e configurados em `AppTargets.kt`/`DrawerCatalog.kt` (ver commit "fix: configura pacotes faltantes...").

## Pendências não-bloqueantes

- Confirmar se dá pra desativar a depuração ADB sem fio pelas Configurações da própria central.
- **Painel em movimento:** CAN validado só com o carro parado (velocidade 0); conferir velocidade e RPM rodando.
- **Apagar tela na central:** conferir se brilho 0 da janela apaga o backlight por completo (no emulador não dá pra ver) e se a central respeita o `FLAG_KEEP_SCREEN_ON` (não suspende com a tela apagada; o toque precisa acender).

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

## Integração CAN bus (`can/`, validada com o motor ligado em 2026-11-07)

O ContentProvider `content://com.autoai.canbus.provider` **não tem dados ao vivo** (é só banco de
protocolos/marcas). Os dados reais vêm de um serviço exportado sem permissão,
`com.can.activity/com.can.ui.CanPopWind` (action `autoai.intent.action.CANBUS_SERVICE`), acessado via
`Binder.transact` direto. O `onBind` devolve um `ICanUI`; o `ICanBus` vem de
`ICanUI.getCanService("CanBusManager")` (código 1) — sem esse passo o serviço recusa com
"Binder invocation to an incorrect interface". Interfaces, códigos e ordem dos campos conferidos no
`com.can.activity` descompilado (APKs extraídos ficam fora do repo, em `../jancar-apks/`).

Origem dos dados (lido no código descompilado): CAN do Fox → caixa Raise (`VW-RZ`) → serial →
`RePQProtocol` (VW plataforma PQ, comando `0x41`) → `CanBusService` → `ICanBusListener`.

- **O Painel usa o CAN como fonte principal** e o ICar de reserva (`DashModel.reading`). Testado na
  central: 0 km/h, 870 rpm, 49,0 L, 34,5 °C, 68.039 km — bate com a tela "Detalhes" original.
- **O `ICar` vem vazio nesta central** (bind ok, mas tudo -1/255: a MCU não publica dados do carro).
  Confirmado em 2026-11-13 com o log cru da MCU: esta central usa o protocolo **JAC_V1** (não o CIS_V2,
  que tem RPM/bateria/portas), e nele a MCU só fala de chave, freio de mão, farol, iluminação,
  relógio, versão e botões aprendidos. Em 5 min com farol, ré, portas, setas e volante, ela só
  respondeu ao acerto do relógio. Farol e ré chegam por fio (GPIO), não pela MCU.
  **Botões do volante vêm pelo app CAN** (`canbus CanKey ... TransKey:Volume_add`) →
  `ivi-services IVICore.onKeyEvent` → broadcast — relevante para a Fase 14.
  Log cru da MCU: linha `global_mcudatadebug` = `true` em `content://com.jancar.settings.provider/settings`
  (`adb shell content update`, sem root) + reiniciar a central; **voltar para `false` depois**.
  `JancarCarClient` (`JancarApp.carService`) serve para o farol (modo noturno), reserva e diagnóstico; **somente leitura**
  (nunca códigos `set*`/`send*`/`upgrade*`).
- Particularidades do CAN: temperatura externa = `ambientTemp − 40`; velocidade fica
  `Int.MIN_VALUE` enquanto nunca saiu de 0 (`CanPopWind.isSpeedValid`), tratada como 0 se o RPM chega;
  `Int.MIN_VALUE` = sem dado; o `DashBoard` **não tem bateria** e o RePQ não preenche autonomia.
- Fios do chicote lidos pelo ICar (GPIO; testado em 2026-11-07, motor ligado): **farol funciona** (`getHeadLightStatus`, código 14; aviso `onHeadLightChanged`, código 7) e comanda o **modo noturno**; **freio de mão não está ligado** (nunca mudou em dois testes — o `hb=1` visto antes veio do desligamento do carro). O CAN não expõe nenhum dos dois.
- Bateria, temperatura do motor e do óleo: **não há fonte** sem root. Ficam presos no app CAN (ou nem são pedidos à caixa: água/óleo = `0x7D` sub 9, que o app nunca pede). O passthrough da MCU tem **um único ouvinte** (`CarService.mIPassthroughDataCallback`): registrar o launcher tiraria os dados do app de fábrica — nunca fazer.
- `CanBusClient` (`JancarApp.canBus`): conexão aberta pelo Painel e pelo Carro 3D, **contada** (`connect()`/`disconnect()`
  em pares; só liga no primeiro e só desliga no último — na troca Painel ↔ Carro as duas telas ficam um instante na
  tela juntas). `JancarCarClient` (`JancarApp.carService`):
  conexão aberta pelo **AppRoot** o tempo todo (farol → modo noturno; loga só quando algo muda). Instâncias únicas; quem só exibe dados usa `CarStateDetails`/`VehicleInfoDetails`, nunca um segundo
  `connect()`/`disconnect()` (derrubaria a conexão de quem abriu). Diagnóstico: toque longo no Painel.
- Usa `kotlinx-coroutines-core` como dependência principal (antes só existia em `testImplementation`).
