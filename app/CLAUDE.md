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
   - Linha de status no topo à direita: **Wi-Fi conectado** = ícone + nome da rede (pedido do usuário, 2026-11-13; some quando desconectado; toque abre as configurações de Wi-Fi; o nome exige `ACCESS_FINE_LOCATION`, concedida por adb — `docs/INSTALL.md` §5.1, senão "Wi-Fi conectado"; `system/WifiStatus`), depois "ADB ativo" e "MULTIMÍDIA"
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
- **Painel** (pedido do usuário, 2026-11-07: "velocidade e RPM grandes, estilo FuelTech"; resto decidido por mim): tela `Screen.Dash` (`ui/dash`), **segue o tema da Home** (claro/escuro, inclusive o modo noturno pelo farol; pedido do usuário, 2026-11-13 — antes era sempre escura). No claro, âmbar/vermelho/azul/verde ficam mais escuros pra continuar legíveis no fundo branco (`DashPalette` em `DashScreen`); as faixas de aviso e a SHIFT mantêm as mesmas cores nos dois temas — barra de RPM segmentada (35 × 200 rpm) com **aviso de troca por modo** (pedido do usuário, 2026-11-10; ele troca por volta de 3000 rpm no dia a dia e 5000 andando bravo): botão no topo do Painel alterna **DIA A DIA** (troca 3000) ↔ **BRAVO** (troca 5000), o modo fica salvo (`SettingsStore.driveMode`). A 500 rpm do ponto (`SHIFT_APPROACH_RPM`) os segmentos acesos e o número do RPM ficam âmbar; no ponto, vermelhos piscando — barra, números da escala na faixa vermelha, número do RPM e faixa SHIFT piscam **juntos**, num pisca só (pedido do usuário, 2026-11-13); só no BRAVO entra a faixa amarela com "SHIFT" em vermelho, à frente de qualquer outro aviso (`DashModel.DriveMode`/`shiftStage`). Escolhido entre 3 modelos (o usuário preferiu "barra e número mudam de cor" a borda da tela acesa ou luzes de troca em cima), velocidade gigante, RPM e, embaixo, só ícones grandes (36dp) com combustível, temperatura externa e odômetro (sem rótulos). **Todos os sensores disponíveis** (pedido do usuário, 2026-11-10: "trazer todos os sensores"): no canto inferior direito, luzes-espia em verde (seta esq., farol, seta dir.; apagadas = quase invisíveis) e o carro visto de cima com portas/porta-malas/capô abertos em âmbar (`DoorsDiagram`). Portas = `Cabin` do CAN (testado: motorista = 1); setas = `Lights` do CAN (**testado no Fox em 2026-11-13**: chega a posição da alavanca, fixa enquanto ligada, então **o Painel pisca o indicador sozinho** (1 piscada/s, pedido do usuário "mais lento"; `TURN_BLINK_HALF_MS`, com 800 ms de folga caso a caixa mande a lâmpada piscando); pisca-alerta = as duas setas em 1, o campo `emergencyFlasher` nunca vem; só chega com o Painel aberto, que é quando o launcher está ligado ao app CAN); farol = fio do chicote (ICar; acende com a lanterna, não distingue farol baixo de alto — o app CAN lê esses bits da caixa mas não os repassa: o `LightInfo` só tem setas e pisca-alerta, sem fonte sem root). Ficam de fora por não terem fonte: bateria, água/óleo, cinto (o PQ não preenche: fica sempre no padrão 1), capô (o PQ não manda; o desenho aceita se vier), marcha (`gearShiftPosition` do PQ só vale com ré + freio de mão, e a câmera de ré já aparece). Prévia com dados falsos (só debug): `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.DashPreviewActivity [--ez moving true] [--ez light true]`. **O mais limpo possível** (pedido do usuário, 2026-11-07): sem bateria, sem autonomia, sem troca de óleo, sem "motor aquecendo"; o status de conexão só aparece se faltarem dados ("Sem dados do carro", após 4 s). **Avisos só quando acontecem** (`DashModel.alerts`, faixa colorida que alterna a cada 3 s): hoje só combustível na reserva (0 < L ≤ 7, âmbar). O aviso de freio de mão andando foi **removido na 1.5.1**: o fio do freio de mão não está ligado nesta instalação (dois testes, nunca mudou). Porta aberta só vira aviso (**vermelho**, primeiro da lista) **andando** a ≥ 5 km/h; parada é normal e o app CAN da central já mostra popup. Temperatura do motor/óleo não existe nesta central (ver seção CAN); a da água vem pelo OBD e está só na tela Fox. O Painel mantém a conexão única do CAN (a do ICar é do `AppRoot`); o diagnóstico usa `CarStateDetails`/`VehicleInfoDetails` (sem conexão própria). Layout compactado para a área útil da central (~534dp: a barra de status da Jancar ocupa o topo). Faixas em `DashModel` (pensadas pro Fox, ajustar lá). Valor desconhecido = "—" em cinza. Inspiração de estilo apenas — sem logo/identidade da FuelTech.
- **Marcha e sugestão de troca** (pedido do usuário, 2026-11-14: "bater o RPM e o km/h e sugerir uma marcha"; câmbio original; `ui/dash/GearModel`, `GearUi`): marcha pela relação RPM ÷ km/h contra as relações do **Fox 1.0 8V (EA111)** das fichas técnicas (Vrum Fox 2011, Autoo Fox 2010 Trend): diferencial 4,929; 2ª 2,30; 3ª 1,433; 4ª 0,975; 5ª 0,776; a 1ª diverge entre fichas (4,167/4,267, usa a média), pneu 195/55 R15 → ~185/101/63/43/34 rpm por km/h. Aceita só dentro de ±11% (4ª↔5ª distam 26%; cobre erro de velocímetro), acima de 6 km/h e 900 rpm; estável por 0,6 s (`GearEstimator`), e some se a leitura passa 0,6 s sem bater (embreagem, ponto morto, reduzindo rápido). Sugestão: **↑** quando o giro entra no âmbar do aviso de troca do modo (DIA A DIA 3000 / BRAVO 5000), **↓** abaixo de 1400 rpm (BRAVO 2500) se a marcha de baixo não passar do ponto de troca. Mostrada em 3 lugares (escolha do usuário): Painel (quadradinho com a marcha + seta ao lado do km/h; ↑ âmbar/vermelho, ↓ azul), visor âmbar da tela Fox (marcha à esquerda da hora, seta à direita, tudo âmbar como o LCD da VW) e cartão sobre o AA (linha "marcha"). **Depende da velocidade do CAN andando, ainda não validada no carro.**
- **Consumo pelo OBD-II no Painel** (pedido do usuário, 2026-11-13: "média instantânea e média da viagem até zerar a trip"; `obd/`, `ui/dash/ConsumptionBlock`): o launcher conversa direto com o **ELM327 Bluetooth** pareado (achado pelo nome: OBD/ELM/vLink…; SPP), **somente leitura**, só PIDs padrão do modo 01 (0B, 0C, 0D, 0F, 10, 52 — usa só os que o carro diz ter em 0100/0120/0140). Consumo é **estimativa** pelo ar admitido (MAF; sem MAF, pressão do coletor + rotação + temperatura do ar com o motor do usuário, **1.0 8V flex, 999 cm³**, eficiência volumétrica 0,80 chutada). Flex: % de etanol do carro (PID 0x52) se ele informar; senão o chip GASOLINA (E27) / MISTURA / ETANOL no Painel (salvo). Andando mostra km/L; parado, L/h. Viagem = litros e km somados até ZERAR (salvos a cada 15 s; intervalos > 5 s sem leitura não contam). **Layout** (revisão de UX, 2026-11-13): coluna do meio do Painel, entre velocidade e giro (AGORA grande, VIAGEM menor + ZERAR); ZERAR pede **dois toques** (1º arma "ZERAR?" âmbar por 3 s); o botão de combustível fica no **topo**, ao lado do modo, e só aparece quando a conta depende dele; sem OBD fica só uma linha discreta (e a viagem guardada, se tiver km). **Conectado sempre** que o launcher roda (escolha do usuário, pela média da viagem): o outro app de OBD da central não consegue usar o adaptador ao mesmo tempo. **Pressão do óleo: não dá** — não existe PID padrão e o motor só tem interruptor (luz), não sensor. Sem OBD: "—" e a linha de status ("OBD: adaptador ELM327 não pareado"/"conectando"). **Ainda não testado no carro.** Prévia: `DashPreviewActivity --ez obd true`.
- **Painel por cima do Android Auto** (pedido do usuário, 2026-11-13: "a view do Android Auto na tela de Painel ou Fox"; `overlay/AutoOverlay`, `OverlayPolicy`): **embutir o AA no launcher não dá** (exibir a tela de outro app exige permissão de app do sistema; copiar a tela pede autorização a cada vez e pesa demais) e a tela dividida é improvável (nada nos apps da Jancar; o AA negocia resolução fixa com o celular). Visual = **modelo 1 "Cartão"** escolhido entre 6 no Claude Design (2026-11-14, https://claude.ai/artifact/1w3Tf19ZwESCtF5pGBerAR): velocidade grande, barrinha de RPM em 12 segmentos, linha do giro e linha do km/L, por cima do AA/CarPlay (`TYPE_APPLICATION_OVERLAY` sem foco). **Arrastável para qualquer lugar como widget** (pedido do usuário): `FLAG_NOT_TOUCH_MODAL`, só a área do cartão recebe toque (o resto vai para o AA), o arraste começa depois de 1,5× o touch slop (toque sem querer não move), borda azul enquanto arrasta, posição salva (`SharedPreferences` `auto_overlay`, distância da direita/topo; padrão canto superior direito), nunca sai da tela. RPM âmbar perto da troca, vermelho piscando no ponto (mesmo modo DIA A DIA/BRAVO); consumo do OBD; linha sem dado some, sem nenhum dado a faixa some. Aparece só com o AA/CarPlay na frente, inclusive quando a central abre o AA sozinha (eventos de uso). Permissões por adb: `SYSTEM_ALERT_WINDOW` e `GET_USAGE_STATS` (`docs/INSTALL.md` §5.2); sem elas, nada aparece. Enquanto aparece segura a conexão contada do CAN. Desenhada num `View` simples (sem Compose fora da activity), 4 quadros/s só enquanto visível. No emulador as Configurações do Android 12+ escondem sobreposições, e o Android 12+ limita a opacidade a 80% (a central é 8.1). **Ainda não testado no carro**. Prévia (só debug, o emulador não tem AA): `adb shell am start -n com.rocha.jancarlauncher/.OverlayPreviewActivity --es pkg com.android.chrome [--es stage near|shift]` e abrir o Chrome; `--ez off true` desliga.
- **Carro** (pedido do usuário, 2026-11-13: tela do seu Fox G2 com portas/luzes seguindo o carro): tela `Screen.Car` (`ui/car3d`), aberta pelo Painel (botão de carro no topo ou toque no desenho das portas); voltar → Painel. **Painel de instrumentos original** (pedido do usuário, 2026-11-14: a tela repetia o Painel; arranjo "A" escolhido no Claude Design, https://claude.ai/artifact/PLemzMUij3ubqgrRhypgYz), redesenhado da foto do painel do dono (`FoxCluster`, `docs/Fox-photos/`, fora do git): conta-giros 0–8 com faixa vermelha a partir de 6,5 à esquerda, velocímetro 0–200 com o relógio do combustível embutido (tanque 50 L) à direita, ponteiros vermelhos animados, aro cromado; o relógio pequeno do conta-giros é a **temperatura da água** 50–90–130 °C como no carro, lida pelo **OBD (PID 05)** (pedido do usuário, 2026-11-14: "temperatura do motor, tem esse sensor?" — o CAN/a central não têm, o ELM327 tem; ficou um tempo como km/L instantâneo); abaixo de 50 encosta no começo, sem OBD ou se o carro não informar = sem ponteiro; luz da lanterna em verde no lugar da do farol (o fio não diz alto/baixo); EPC/bateria/injeção/freio de mão ficam de fora (sem fonte). Visor âmbar no meio como o do carro (ponto decimal e "---" sem dado): hora, consumo médio da viagem (OBD), temperatura externa, km e trip (km da viagem do OBD); setas verdes dos lados, **48dp** (pedido do usuário: "aumentar as setas"). Tela sempre no tema escuro. Sem a faixa de RPM do Painel (`CarGauges` removido); o aviso de troca aqui é um **shift light só no conta-giros** (pedido do usuário, 2026-11-14): 8 LEDs embutidos no topo do aro (3 verdes, 3 âmbar, 2 vermelhos) acendem um a um nos 1500 rpm antes do ponto de troca do modo (DIA A DIA 1500→3000, BRAVO 3500→5000) e, no ponto, todos vermelhos piscando (mesmo pisca do Painel); fora da faixa o aro fica cromado como o original. Botão de modo DIA A DIA/BRAVO no topo da tela (o mesmo do Painel, salvo em `SettingsStore.driveMode`). Acima do visor, a **foto real do carro** (`assets/car/photo.png`, `CarPhoto`): recortada do fundo, placa em branco, 3/4 pela frente do lado do motorista; por cima, só o que vem de sinal real — brilho nos faróis (fio de iluminação), setas/pisca-alerta piscando (CAN `Lights`) e destaque âmbar nas portas abertas visíveis (CAN `Cabin`); portas fora do ângulo ficam no texto de baixo. **Histórico**: começou como 3D próprio em OpenGL ES 2.0 (a GPU é Mali-450, só ES 2.0; medido na central: ~68 MB de PSS, ~37 fps), refinado até ~40 mil triângulos, mas o usuário achou pouco realista e escolheu a foto (não existe modelo 3D do Fox G2 à venda); o código 3D foi removido. Refazer/ajustar o recorte ou trocar a foto: `tools/car-photo/LEIA-ME.md`. Prévia com estados falsos (só debug): `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.CarPreviewActivity [--es open fl,fr,rl,rr,trunk,hood] [--ez lights true] [--es turn left|right|hazard] [--ez light true] [--ei rpm N] [--ei speed N] [--ei fuel N] [--ez obd true] [--es mode Daily|Sport]`; **modo DEMO** (pedido do usuário, 2026-11-14): `--ez demo true` roda em loop um roteiro de 32 s (`FoxDemo`, só debug, etiqueta DEMO na tela) com porta abrindo, farol, aceleração com trocas de marcha, setas, pisca-alerta, frenagem e porta-malas.
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
- Bateria e temperatura do óleo: **não há fonte** sem root. **Temperatura da água do motor: só pelo OBD (ELM327, PID 05)**, usada no relógio da tela Fox; pelo CAN/central continua sem fonte. Ficam presos no app CAN (ou nem são pedidos à caixa: água/óleo = `0x7D` sub 9, que o app nunca pede). O passthrough da MCU tem **um único ouvinte** (`CarService.mIPassthroughDataCallback`): registrar o launcher tiraria os dados do app de fábrica — nunca fazer.
- `CanBusClient` (`JancarApp.canBus`): conexão aberta pelo Painel e pela tela do Carro, **contada** (`connect()`/`disconnect()`
  em pares; só liga no primeiro e só desliga no último — na troca Painel ↔ Carro as duas telas ficam um instante na
  tela juntas). `JancarCarClient` (`JancarApp.carService`):
  conexão aberta pelo **AppRoot** o tempo todo (farol → modo noturno; loga só quando algo muda). Instâncias únicas; quem só exibe dados usa `CarStateDetails`/`VehicleInfoDetails`, nunca um segundo
  `connect()`/`disconnect()` (derrubaria a conexão de quem abriu). Diagnóstico: toque longo no Painel.
- Usa `kotlinx-coroutines-core` como dependência principal (antes só existia em `testImplementation`).
