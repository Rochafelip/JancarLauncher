# Scanner de falhas, motor esquentando, viagens, ajustes, atualização e relatório — design

2026-11-17. Continuação de `2026-11-17-custo-bateria-velocidade-cor-design.md` (aprovado pelo dono no mesmo dia,
junto com este). Origem: comparação com o Rota Launcher 0.15.6; itens que o dono marcou como "vale muito" e "vale".
Só a ideia vem do Rota: textos, tabela de códigos e código são nossos.

## Decisões do dono (2026-11-17)

- Tudo num plano só: estes itens + custo, bateria, velocidade e cor.
- Viagens anteriores: **uma viagem por ligada do carro**; a média do ZERAR continua como está (para o tanque).
- Atualização sem ADB: **GitHub (última release) e pendrive**.
- Scanner: **só ler** (o OBD continua somente leitura; nada de modo 04).

## Por quê

- Scanner: saber o que a luz de injeção quer dizer sem levar a um scanner/oficina.
- Motor esquentando: o dado (água, PID 05) já chega e hoje só move um ponteiro.
- Viagens: ver quanto cada ida gastou (km, tempo, km/L, R$).
- Retomar música: ligar o carro e a música voltar sozinha.
- Atualização e relatório de erros: o ADB sem fio cai toda hora; instalar e depurar sem ele.

## Anti-objetivos

- Scanner usável andando; apagar códigos; textos de diagnóstico que afirmam "peça X com defeito".
- Painel mais poluído (avisos só quando acontecem).
- Atualização que instala APK de outra assinatura ou versão mais velha.

## Restrições novas

- O socket do ELM327 é único e é do `ObdClient` (loop de leitura a cada 250 ms). Qualquer comando novo (ATRV,
  modos 03/07/0A) passa por ele, sem segunda conexão.
- Protocolo do Fox (CAN ISO 15765 ou KWP ISO 14230) não confirmado: `ATSP0` escolhe sozinho; as respostas do modo 03
  vêm em formatos diferentes em cada um (o parser aceita os dois).
- O app **não tem `INTERNET`** hoje; a atualização precisa dela e de `REQUEST_INSTALL_PACKAGES` (fontes
  desconhecidas liberadas para o launcher; na Jancar, por adb uma vez: `appops set ... REQUEST_INSTALL_PACKAGES allow`).
- Releases (`app-release.apk`) são assinadas com a chave **debug** da máquina do dono (`signingConfig = debug`).
- Não existe tela de Ajustes; configurações hoje ficam no painel de brilho ("Escolher tema").

## Design

### Tela OBD (entrada nova)

`Screen.Obd`, aberta por um botão no topo do Painel (ícone de chave inglesa). Abas **Viagens · Falhas**. Segue o
estilo do Painel (foto escurecida, cartões translúcidos, tema claro/escuro). Voltar → Painel.

### Scanner de falhas (vale muito)

- Botão **LER FALHAS** (só com o carro parado: velocidade ≤ 3 km/h no CAN/OBD; andando, "Disponível com o carro
  parado").
- `ObdClient` ganha uma **fila de comandos**: o scanner pede, o loop pausa no fim do ciclo, roda 0101 (luz de injeção +
  número de códigos), 03 (confirmados), 07 (pendentes), 0A (permanentes; "não suportado" é normal) e volta ao loop.
- Parser puro de DTC: bytes → `P0301` etc. (letra pelos 2 bits altos), aceitando resposta com e sem cabeçalho/contagem
  (CAN e KWP).
- Tabela própria em português (`assets/obd/dtc_pt.json`), escrita por nós: ~120 códigos genéricos P0 mais comuns
  (falha de combustão, sondas, mistura, MAP/MAF, TPS, temperatura, catalisador, EVAP, ignição, injetores, velocidade,
  marcha lenta, tensão). Cada código: título, gravidade (`baixa`/`média`/`alta`), possíveis causas, o que conferir.
  Código fora da tabela: genérico P0 = "sem descrição nesta versão"; P1/fabricante = "código do fabricante (VW)".
- Resultado: luz de injeção acesa/apagada; lista por código com etiqueta CONFIRMADO/PENDENTE/PERMANENTE e cor da
  gravidade; toque abre o detalhe. Aviso fixo: "um código indica a condição detectada, não confirma peça com defeito".
- Histórico: últimas 10 leituras (data, luz, códigos) em `filesDir/obd/scans.json`.
- Prévia de debug: modo DEMO com códigos falsos.

### Motor esquentando (vale)

- `DashModel.Alert.Overheat` (vermelho): água ≥ 110 °C por 10 s; sai abaixo de 105. Faixa/cartão do Painel com
  "MOTOR ESQUENTANDO · 112 °C". No cartão sobre o AA, uma linha vermelha "ÁGUA 112 °C" enquanto durar.

### Viagens anteriores (vale)

- Viagem começa quando o OBD conecta e o carro anda; termina quando a sessão OBD cai (ignição desligada) e não volta
  em 3 min (parada rápida não divide).
- Registro: início, fim, tempo andando, km, litros, litros de etanol equivalentes (para o custo do item 2), km/L,
  velocidade média (km ÷ tempo andando) e máxima. Últimas 20 em `filesDir/obd/trips.json`; viagens com < 0,3 km não
  entram.
- Aba Viagens: a atual no topo (ao vivo) e as anteriores em lista: data/hora, km, tempo, km/L, R$ (se houver preço),
  média/máxima. Sem gráfico.

### Ajustes do launcher (base para os próximos)

- `Screen.Settings`, aberta por "Ajustes do launcher" no painel de brilho (ao lado de "Escolher tema").
- Itens: Retomar a música ao ligar (liga/desliga) · Atualizar o launcher · Relatório de erros · Versão instalada.

### Retomar a música ao ligar (vale testar)

- Passo 0, no carro: ligar com música tocando e ver se a central retoma sozinha. Se retomar, o item não é feito.
- Se não: ao ligar (boot ou acordar da suspensão, os mesmos gatilhos da Boas-vindas), depois de 4 s, se a última
  fonte tocando era a Música da Jancar → play pelo `JancarMediaClient`; se era uma sessão padrão → `play()` no
  `MediaController` dela. Guardar a "última fonte tocando" ao parar/desligar. Padrão **desligado** até testado.
- Com o Android Auto/CarPlay conectando, não mexe (o celular decide).

### Atualizar o launcher (vale)

- **GitHub:** `GET https://api.github.com/repos/Rochafelip/JancarLauncher/releases/latest` (sem login, sem dados do
  dono), pega o asset `.apk`, compara `tag` com a versão instalada; mostra "Versão X disponível · Baixar".
- **Pendrive:** "Instalar de um arquivo" abre o seletor de arquivos do Android (SAF); se a central não tiver seletor,
  procura `*.apk` nas raízes de `/storage` (pede `READ_EXTERNAL_STORAGE`, concedida por adb).
- Antes de instalar, sempre: `getPackageArchiveInfo` → mesmo pacote, **mesma assinatura** do instalado e
  `versionCode` maior. Senão recusa com o motivo.
- Instala por `PackageInstaller` (sessão) ou intent de instalação com `FileProvider`; o Android mostra a tela de
  confirmação. A atualização mantém HOME e preferências (como o `adb install -r`).
- Permissões novas: `INTERNET`, `REQUEST_INSTALL_PACKAGES`. `docs/INSTALL.md` ganha o passo do `appops`.

### Relatório de erros (vale)

- O `CrashRestarter` passa a gravar cada crash em `filesDir/crash/<data>.txt` (pilha + versão + últimas linhas do
  registro abaixo); guarda os 10 mais novos.
- Registro em memória (anel de 200 linhas) com eventos importantes: OBD conectou/caiu/erro, CAN conectou/caiu,
  atualização, aviso disparado. Escrito junto no crash.
- Em Ajustes: lista dos crashes (data, primeira linha) → texto completo rolável; botão "Apagar tudo" (dois toques).
  "Registro agora" mostra o anel atual. Fonte de 14sp+ para dar para fotografar a tela.

## Ordem de construção (escolha minha)

1. Relatório de erros + tela Ajustes (ajuda a depurar o resto no carro).
2. Atualizar o launcher (as próximas versões entram sem ADB).
3. Fila de comandos do OBD + ATRV (bateria, item 3) + motor esquentando.
4. Scanner de falhas.
5. Custo (item 2) + viagens anteriores (mesmos dados da viagem).
6. Aviso de velocidade (item 4).
7. Retomar a música (depois do teste no carro).
8. Cor do Fox (item 6).

## Para quem constrói

- Formato exato do JSON da tabela, layout fino das abas e dos itens de Ajustes, textos curtos.
- Escolher `PackageInstaller` vs intent pelo que funcionar na API 27 da central.
- Testes puros: parser de DTC (CAN e KWP), regras de aviso, divisão de viagens, comparação de versão/assinatura.

## Testar no carro

- Modos 03/07/0A no adaptador e no protocolo do Fox; luz de injeção (0101).
- Seletor de arquivos e pendrive na Jancar; instalar por cima pela tela do Android.
- Se a central já retoma a música sozinha.
