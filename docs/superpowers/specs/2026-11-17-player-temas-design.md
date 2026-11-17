# Player de música ou rádio em todos os temas da Home — design

2026-11-17. Pedido do dono: "revise cada tela dos temas, e adicione a área de player de música para cada tela de HOME;
quero o player de música ou de rádio". Uso (dono): **ver o que está tocando e trocar faixa ou estação**.

## Revisão das 12 telas (emulador, 1024×600, sem mídia tocando)

- Sem player nenhum: C Coluna, E Central, G Grade, H Trilho, J Oito, K Pílulas, L Vitrine.
- Player que some quando nada toca: A Atual, B Dock, F Carro (o card só aparece com faixa).
- D Tocando e I Widgets: "Nada tocando" abre só a Música; não há atalho para o Rádio.
- I Widgets: a data quebra em duas linhas dentro do cartão do relógio.
- Nada de sobreposição, texto cortado ou toque pequeno nos demais.

## Objetivos e não-objetivos

- Objetivo: todo tema com uma área de player **sempre visível**: o que toca (faixa/artista ou estação) e
  ⏮ ⏯ ⏭ (no rádio, trocam a estação). Sem nada tocando: "Nada tocando" + atalhos **Música** e **Rádio**.
- Não-objetivos: lista de estações, busca de frequência, controle de volume, seleção de fonte dentro do player.
- Anti-objetivos: abrir o serviço próprio do rádio (`IRadio.open`) — pode disputar o controle com o app de Rádio da
  central, como o passthrough da MCU; esconder o carro da foto; botões menores que 48dp.

## Abordagens

- **A (escolhida pelo dono):** um componente de player com dois tamanhos — **Cartão** (temas com espaço) e **Barra**
  (temas apertados) — com a mesma fonte e os mesmos botões em todos os temas.
- B: o card "Tocando agora" atual em todos: alto demais (~120dp) para C, H, K, L sem tirar atalhos ou cobrir o carro.
- C: blocos separados de Música e de Rádio: um sempre vazio, o dobro do espaço.

## Fonte dos dados

- Mesma regra de hoje (`JancarNowPlaying.pick`): Música da Jancar (serviço `IMedia`) ou sessão padrão (Bluetooth,
  apps). Transporte igual ao card atual (`onPlay/onPause/onNext/onPrevious`).
- **Rádio:** o serviço `IMedia` tem `mediaType` com `RADIO = 1` (`IVIMedia.MediaType`, `ivi-services` descompilado).
  Quando `mediaType == 1`, o player mostra o ícone do rádio, o texto do serviço como título (frequência/estação) e
  "Rádio" como subtítulo; ⏮ ⏭ usam o mesmo transporte do serviço (troca de estação **a confirmar no carro**).
- Para conferir no carro: o `JancarMediaClient` passa a registrar no `RingLog` cada mudança de faixa com tipo, título
  e subtítulo (`midia: tipo=1 t1=… t2=…`), visível em Ajustes › Relatório de erros › Registro agora.
- Se o rádio **não** publicar no `IMedia`: o player fica em "Nada tocando" e o atalho Rádio abre o app; sem nova conexão.

## O componente

- `PlayerCard` (Cartão): ~112dp de altura, largura do lugar (420–560dp). Capa 72dp (ou ícone ♪ / rádio), título
  (1 linha), subtítulo (1 linha), ⏮ ⏯ ⏭ de 56dp à direita.
- `PlayerBar` (Barra): 64dp de altura, 380–520dp. Ícone/capa 44dp, "título · subtítulo" numa linha, ⏮ ⏯ ⏭ de 48dp.
- Ocioso (nada com título): ícone ♪, "Nada tocando" e dois botões de 48dp+ **Música** · **Rádio** no lugar dos de
  transporte. Toque no texto abre o app da fonte (Música da Jancar ou o app da sessão; rádio → app de Rádio).
- Pausado com faixa: mostra a faixa com ⏯ em "play" (como o card atual).
- Superfície igual à dos blocos do tema (translúcida sobre a foto, `OnPhotoSurfaces`).

## Lugar em cada tema

| Tema | Tamanho | Onde |
| --- | --- | --- |
| A Atual | Cartão 560 | no lugar do card atual (acima dos chips), agora sempre visível |
| B Dock | Cartão 560 | acima do dock, à esquerda (lugar atual), sempre visível |
| C Coluna | Barra (largura da coluna) | na coluna escura, embaixo dos chips |
| D Tocando | Cartão (largura atual) | no lugar do "Nada tocando"/card |
| E Central | Barra 480 | centralizada, logo acima da fileira de botões redondos |
| F Carro | Cartão 460 | lugar atual, sempre visível |
| G Grade | Cartão 420 | canto superior esquerdo, embaixo dos chips (espaço livre sobre o para-brisa) |
| H Trilho | Barra 420 | à direita, embaixo do relógio |
| I Widgets | Cartão (célula atual) | no lugar do "Nada tocando"/card; data do relógio numa linha só (texto menor ou abreviado) |
| J Oito | Barra 560 | acima da fileira dos 8 botões, à esquerda |
| K Pílulas | Barra 420 | à esquerda, embaixo do relógio |
| L Vitrine | Barra 480 | centralizada, logo acima do dock |

O véu da foto de cada tema (`HomeLayout.veil`) é ajustado onde a Barra/Cartão entra sobre a foto, para o texto seguir
legível. As miniaturas da galeria (`HomeLayout.preview`) ganham o bloco do player.

## Para quem constrói

- Medidas finas (paddings, posições exatas) conferidas por captura no emulador nos 12 temas, claro e escuro, com e sem
  mídia (`ThemeSetActivity` + uma prévia com faixa falsa e com rádio falso).
- Regra pura e testada do que o player mostra (`PlayerModel`: música / rádio / ocioso, título/subtítulo, ícone,
  rótulos de acessibilidade).

## Testar no carro

- Rádio ligado: o `RingLog` mostra `tipo=1` e o player mostra a estação? ⏮ ⏭ trocam a estação?
