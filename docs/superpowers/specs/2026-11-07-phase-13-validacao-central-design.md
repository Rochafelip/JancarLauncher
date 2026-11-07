# Fase 13 — Validação na central real + calibração do CAN bus (spec)

## Objetivo

Tudo até a Fase 12 foi construído e testado só em emulador (Android 15, API 35, 1024×600 @160dpi)
— `ledger.md` e `DONE.md` marcam dezenas de itens como "NÃO verificado"/"⚠" por depender da
central real (API 27, ARMv7). Esta fase é a primeira rodada completa na central de verdade, mais
a calibração do cliente de CAN bus (`can/CanBusClient.kt`, adicionado em 2026-11-07, ainda não
validado com dados reais do carro).

Não é uma fase de código novo — é uma fase de **verificação**: rodar o que já existe, anotar o
resultado (igual ao padrão do `ledger.md`: "Evidência" sempre diz exatamente o que foi visto), e só
mexer em código onde a central mostrar um problema real.

## Requisitos

Cobrir, nesta ordem (a ordem importa: instalar e virar HOME primeiro, senão nada mais dá pra testar
no app de verdade):

1. Instalação limpa seguindo `docs/INSTALL.md` do começo ao fim, incluindo `set-home-activity` e o
   tempo de espera antes de reiniciar (Fase 6 encontrou ~25s pra gravar em disco).
2. Boot como HOME: reiniciar a central de verdade, confirmar que o launcher abre sozinho, sem tela
   branca, sem recriar a Activity à toa.
3. Os 4 blocos principais abrindo os apps reais (Android Auto, CarPlay, Rádio, Música, Vídeo) —
   nenhum deles foi aberto de verdade ainda, só testado com toast de "não instalado" no emulador.
4. Detecção Auto vs. CarPlay com um celular de verdade pareado (Bluetooth) e por cabo (USB).
5. Câmera de ré: confirmar se `com.autochips.backcarapp` aceita abertura manual fora da marcha-ré
   (pendência desde a Fase 4).
6. Brilho: testar se `Settings.System` aceita escrita (`WRITE_SETTINGS`) na central, ou se o
   fallback de janela (já funcional) é o caminho definitivo.
7. Boas-vindas ao ligar o carro: confirmar que desligar/ligar a central dispara tela apagada/acesa
   do jeito que o app espera (a central pode dormir diferente de um celular/tablet).
8. Memória e CPU reais (a central é 2GB RAM, ARMv7 — o emulador testado era x86 com renderização
   por software, não representativo).
9. "Tocando agora": conceder acesso a notificações manualmente e confirmar com um player real
   tocando (capa, título, artista, comandos de transporte).
10. Indicador de ADB: confirmar se `/proc/net/tcp` é legível pelo app na central (pode ser diferente
    do emulador).
11. Modo noturno automático: confirmar a transição real perto de 20:00/05:30 (ou ajustar o relógio
    da central pra forçar, se for mais prático).
12. Calibrar `can/CanBusClient.kt`: usar o `VehicleInfoCard` (hoje temporário em `HomeScreen.kt`) +
    `adb logcat -s CanBusClient` com o carro ligado. Comparar velocidade/RPM com o painel físico,
    abrir/fechar portas, ligar setas, puxar o freio de mão — um por um, anotando se o valor lido
    bate com a ação feita. Se o campo `restante` (bytes sobrando no Parcel) for sempre `0`, a ordem
    dos campos está certa; se não for, corrigir a ordem em `readDashboard`/`readCabin`/`readLights`.
13. Confirmar se existe algum caminho nas Configurações da própria central pra desativar a
    depuração ADB sem fio (pendência desde a Fase 0 do mapeamento original).

## Decisões minhas

- Não vou tentar reproduzir tudo isso num AVD API 27 antes — não existe imagem de sistema API 27
  para o Android Studio atual (só 35/37, registrado desde a Fase 1); a central é o único jeito de
  testar a versão real.
- Itens que dependem de hardware que eu não tenho controle (Bluetooth/USB com celular real, ligar o
  motor pra testar câmera de ré) ficam documentados como "não verificado" no `ledger.md` em vez de
  bloquear a fase inteira — cada um é independente.
- A calibração do CAN bus é tentativa-e-erro por natureza (a ordem dos campos foi inferida de
  bytecode, não documentada pelo fabricante): não é erro de implementação se a 1ª tentativa não
  bater, é o processo esperado.

## Fora de escopo

- Qualquer ajuste visual/de UX que não seja consequência direta de um problema achado na central
  (isso é uma fase de validação, não de polimento — polimento novo vira outra fase).
- Decompilar o `.odex`/`.vdex` do `com.can.activity` pra confirmar a ordem dos campos por código em
  vez de calibração empírica — só se a calibração por tentativa-e-erro não convergir.
- Remover definitivamente o `VehicleInfoCard` de `HomeScreen.kt` (ele sai da tela de produção só
  depois que os dados estiverem confiáveis — pode ser nesta fase ou na seguinte).

## Aceite

1. `docs/superpowers/ledger.md` ganha uma seção "Fase 13" com evidência de cada item da lista de
   Requisitos (reiniciado, visto no logcat, medido, etc.) — nada fica marcado "feito" sem dizer o
   que foi observado, seguindo o padrão já usado nas Fases 1-12.
2. `docs/superpowers/DONE.md`: a tabela de critérios troca ⚠/◐ por ✅ (ou documenta por que não deu,
   se for o caso) para os itens que dependiam da central.
3. `app/CLAUDE.md`: a seção "Integração CAN bus" atualizada com o resultado da calibração (ordem dos
   campos confirmada ou corrigida) e a pendência de câmera de ré/ADB resolvida ou reformulada.
4. Pelo menos um commit cobrindo qualquer correção de código que a central exigir (ex: ordem errada
   num campo do CAN bus, ajuste de permissão).
