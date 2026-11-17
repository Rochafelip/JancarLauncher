# Custo da viagem, bateria, aviso de velocidade e cor do Fox — design

2026-11-17. Origem: comparação com o Rota Launcher 0.15.6 (APK descompilado em `../jancar-apks/rota-launcher/jadx`).
O dono escolheu, da lista de ideias, os itens 2 (custo em R$), 3 (saúde da bateria), 4 (aviso de velocidade) e
6 (troca de cor). Nada do Rota é reaproveitado (imagens, textos longos, código): só a ideia.

## Por quê

App de um carro só (o Fox 1.0 8V flex do dono), uso diário na central Jancar 8227L. Os quatro itens reaproveitam
dados que já chegam (OBD, CAN, foto do carro) e não precisam de internet.

- **Custo:** saber quanto a viagem custou em reais, não só em litros.
- **Bateria:** saber cedo se o alternador parou de carregar ou se a bateria está fraca.
- **Velocidade:** um bipe acima de um limite escolhido.
- **Cor:** "pra ver como ficaria em outras cores, só por diversão" (palavras do dono). Não muda tema nem cores do app.

## Objetivos, não-objetivos e anti-objetivos

- Objetivos: os quatro recursos funcionando com os dados de hoje, testáveis no emulador pelas prévias de debug.
- Não-objetivos: preço por posto/histórico de abastecimento; número da bateria fixo na tela; aviso de velocidade com
  o launcher totalmente fechado (serviço próprio); cor livre com seletor; trocar a cor da foto da Home/Boas-vindas
  (fica para depois, se o dono gostar).
- Anti-objetivos (falharia mesmo funcionando): Painel mais poluído (decisão de 2026-11-07: "o mais limpo possível",
  avisos só quando acontecem); bipe que apita sem parar ou com leitura ruim do CAN; carro tingido inteiro (vidros,
  pneus e faróis coloridos) com cara de adesivo.

## Restrições

- Central: API 27, 2 GB de RAM, Mali-450 (só GLES 2.0), 1024×600 @160. Sem root.
- **Bateria não tem fonte no CAN nem no ICar** (o `DashBoard` não traz bateria; o ICar vem vazio nesta central).
  Única fonte: o **ELM327**, comando `ATRV` (tensão no pino 16 do conector OBD). Clones costumam errar ±0,3 V.
- Velocidade: CAN (`DashModel.reading`) **ainda não validada andando**; o OBD (PID 0D) também traz velocidade e fica
  conectado sempre que o launcher roda (`JancarApp.obd`).
- O CAN só fica aberto com Painel, Carro, temas F/I ou o cartão sobre o AA na tela (`CanBusClient`, contado).
- Consumo é estimativa pelo ar admitido (`FuelModel`); a mistura flex pode mudar no meio da viagem.
- Foto do carro: `assets/car/photo.png` (prata, fundo transparente, gerada por `tools/car-photo/recortar.ps1`).

## Abordagens consideradas

**Custo (2)**
- a) litros da viagem × preço atual na hora de mostrar: simples, mas ignora a troca de mistura no meio da viagem.
- b) somar R$ a cada intervalo com o preço da hora: certo com troca de mistura, mas só conta a partir de quando o
  preço foi definido e mudar o preço não corrige o passado.
- c) **escolhida:** guardar, além dos litros, os "litros de etanol equivalentes" e calcular o custo na hora de
  mostrar. Como o preço é linear na fração de etanol, fica certo com troca de mistura **e** com preço mudado depois.

**Bateria (3)**
- a) número da tensão fixo no Painel: contraria o "Painel limpo".
- b) **escolhida:** só aviso quando sai da faixa (faixa de aviso do Painel) e a luz da bateria acesa na tela Fox,
  como a luz-espia do painel original.

**Aviso de velocidade (4)** — o dono escolheu **A**: sem serviço novo, usa as conexões que já existem.
- A) **escolhida:** regra no escopo do app, alimentada pela velocidade que já chega (CAN quando aberto, senão OBD).
- B) serviço em segundo plano com o CAN sempre aberto: mais trabalho e mais risco na central.
- Nota: como o OBD fica conectado sempre que o launcher roda, o A já apita na maior parte do tempo (inclusive com o
  AA na frente), sem o custo do B.

**Cor (6)**
- a) mudar o matiz da foto inteira, sem máscara: rápido, mas pinta vidros, pneus e faróis (anti-objetivo).
- b) uma imagem pronta por cor: pesa no APK e na memória, e não deixa ajustar.
- c) **escolhida:** máscara da lataria feita uma vez + tingimento na hora de desenhar (`ColorFilter` de matriz).
  Mantém reflexos e sombras do prata.

## Design

### 2. Custo da viagem em R$

- `Trip` ganha `ethanolLiters: Double` = Σ litros do intervalo × g(f), com f = fração de etanol usada naquele
  intervalo (PID 52 se o carro informa, senão a mistura escolhida) e g(f) = (f − 0,27) / 0,73, limitado a 0..1
  (0 = gasolina E27, 1 = etanol). Zerar a viagem zera os dois. Persistido com os outros (`trip_ethanol_liters`).
- Custo = Pg × litros + (Pe − Pg) × ethanolLiters, com Pg/Pe = preço do litro da gasolina/etanol. Função pura.
- Preços em `SettingsStore` (centavos, `Int`); sem preço definido → custo não aparece.
- **Onde aparece:** só no bloco VIAGEM do Painel, linha pequena embaixo do km/L: `R$ 12,40`. Sem preço:
  `R$ — definir preço` em cinza. O visor da tela Fox fica fiel ao carro (sem R$); o cartão sobre o AA não muda.
- **Definir preço:** toque na linha do R$ abre um painel pequeno sobre o Painel:
  ```
  PREÇO DO LITRO
  Gasolina   [−0,10] [−]  R$ 6,19  [+] [+0,10]
  Etanol     [−0,10] [−]  R$ 4,29  [+] [+0,10]
                                   [ OK ]
  ```
  Botões de 56dp, segurar repete. Sem teclado (dirigindo o teclado é ruim). Fecha no OK ou tocando fora.

### 3. Saúde da bateria

- `ObdClient` manda `ATRV` a cada 5 s (fora do ciclo rápido, como a água) e publica `Live.batteryV`.
  Parser puro para respostas tipo `12.6V`/`14.1 V`; resposta estranha = sem dado.
- Regra pura `BatteryRule` (entra o histórico curto de tensão e o giro do CAN/OBD):
  - motor ligado (giro > 500) há mais de 30 s e tensão < 13,0 V por 20 s → **BATERIA NÃO CARREGA** (vermelho;
    detalhe "12,4 V com o motor ligado").
  - motor ligado e tensão > 15,0 V por 20 s → **TENSÃO ALTA** (vermelho).
  - motor desligado, ignição ligada, tensão < 12,2 V por 20 s → **BATERIA FRACA** (âmbar).
  - os primeiros 10 s depois do motor ligar não contam (queda da partida). Sai do aviso com 0,3 V de folga.
- Entra como `DashModel.Alert` (faixa/cartão do Painel, conforme o estilo escolhido) e acende uma **luz de bateria
  vermelha** no conta-giros da tela Fox (só acesa com aviso; sem dado não desenha).
- Constante `ATRV_OFFSET_V = 0f` para calibrar com um multímetro, se o adaptador errar.

### 4. Aviso de velocidade

- Limite em `SettingsStore`: desligado (padrão) ou 40..150 km/h de 10 em 10.
- **Onde escolher:** chip no topo do Painel, ao lado do modo: `LIMITE 110` / `SEM LIMITE`. Toque abre um painel com
  `[−] 110 km/h [+]` e `Desligar`.
- Regra pura `SpeedAlertRule`: dispara quando a velocidade fica ≥ limite + 2 por 2 s → 2 bipes curtos; enquanto
  continuar acima, 1 bipe a cada 20 s; rearma quando cai abaixo de limite − 5.
- Fonte: velocidade do CAN quando aberto; senão a do OBD. Roda num objeto do `JancarApp` (vida do processo), sem
  serviço novo.
- Visual: enquanto acima, o número da velocidade fica vermelho no Painel e no cartão sobre o AA.
- Som: `ToneGenerator` (bipe curto). Fluxo de áudio a confirmar no carro com música do AA tocando (primeira
  tentativa: `STREAM_MUSIC`).

### 6. Cor do Fox

- Novo asset `assets/car/body_mask.png` (mesmo tamanho da `photo.png`; alfa = lataria). Gerado por um script novo
  `tools/car-photo/mascara.ps1`: lataria = pixels do carro com pouca saturação, menos polígonos de vidros, rodas/pneus,
  faróis, lanternas, grade, frisos e placa medidos com `grade.ps1`; borda suavizada 1–2 px. Prévia sobre xadrez e
  com uma cor de teste para conferir.
- Ao carregar a foto (fora da thread da tela), monta uma vez a "camada da lataria" = foto × máscara.
- Desenho: foto, depois a camada da lataria com `ColorFilter.colorMatrix` da cor escolhida (luminância → cor,
  ganho e base por cor, para o preto continuar com reflexo e o branco não estourar), depois luzes e balões como hoje.
  "Original" não desenha a camada.
- Cores prontas (nomes genéricos, sem nomes de fábrica): Original, Vermelho, Preto, Branco, Grafite, Azul-marinho,
  Azul-claro, Amarelo, Verde-limão, Laranja.
- **Onde escolher:** toque na foto da tela Fox → fileira de bolinhas de cor sobre a parte de baixo da caixa da foto;
  some após 6 s sem toque ou tocando fora. Escolha salva em `SettingsStore` (`car_paint`).
- Memória: a camada extra tem o tamanho da foto (~4 MB). Só na tela Fox.
- Prévia de debug: `CarPreviewActivity --es paint Vermelho`.

## O que fica para quem constrói

- Ganho/base de cada cor (ajuste visual olhando a prévia).
- Os polígonos exatos da máscara e o limiar de saturação.
- Layout fino dos dois painéis pequenos (preço e limite), seguindo `Dimens` e o tema do Painel.
- Testes: unidades puras (custo, g(f), parser do ATRV, `BatteryRule`, `SpeedAlertRule`, matriz da cor) com TDD.

## Não testado / testar no carro

- `ATRV` no adaptador do dono (resposta e precisão contra multímetro).
- Bipe audível com música do AA e volume da central.
- Velocidade do CAN andando (pendência antiga) e do OBD.
