# Verificação da câmera de ré no carro (2026-11-16)

## Objetivo

Descobrir tudo que falta para dois pedidos do dono:

1. **A música não abaixar quando entra a ré.**
2. **Editar a câmera de ré, principalmente as linhas.**

Nada aqui muda a central. É só olhar, fotografar e copiar arquivos. As mudanças ficam para depois,
com cópia de segurança.

## O que já se sabe pelo código (2026-11-15, sem testar no carro)

Tirado do `ivi-services` descompilado (`jancar-apks\ivi-services\jadx\sources`).

| Assunto | O que o código diz |
|---|---|
| Quem detecta a ré | Fio de ré do chicote (GPIO): `Platform_AutoChips_8227l_*.getCcdStatus()`. A CAN só manda o aviso de ré (`action_backcar_notification`) em outras marcas, não no Raise PQ. |
| Quem mostra a câmera | O serviço `com.autochips.camera/.CameraService` sobe no boot (`MainService`). A tela é `com.autochips.camera.ui.activity.MainActivity`. Há também a "ré rápida" do boot (`persist.action.fastreverse.state`), que mostra a câmera antes do Android terminar de ligar. |
| Avisos de ré | Broadcasts `android.backcar.action.PREPARE_START`, `STARTED` e `FINISH`. ICar: `getCcdStatus` (código 10) e o callback `onCcdChanged` (código 3). |
| Música na ré | `AudioService.updateMainVolume`: na ré, o volume geral vai para `AudioParam` 15 (`VOLUME_MEDIA_CCD`), de 0 a 100, **padrão 40%**. Pode ser mudado por `IAudio.setParam(15, 100)` (transação 9, sem checagem de permissão; bind com a action `com.jancar.services.action.audio`). A tela de ajuste tem um item `MediaCCDVolume` que pode estar escondido pelo `ivi-settings.ini` (`[Audio]`). |
| Linhas da ré | Lidas **pelo app da câmera**, não pelo serviço, a partir do `/jancar/config/ivi-config.ini` (que substitui o `/etc/ivi-config.ini`). Seção `[car_ccd_line]`: `car_width`, `car_wheelbase`, `car_front_track_width`, `car_rear_track_width`, `car_rear_len`, `camera_r`, `track_max_len`, `track_min_len`, `car_steering_wheel_angle_max`, `car_wheel_angel_max`, `disp_style`, `pts00`…`pts31`. **Os valores padrão são de um Golf 7** (1799 mm de largura, 2637 mm de entre-eixos). Seção `[CarCcd]`: `TRACK_OFFSET_X/Y`, `OnlySaticTrackByImg`. |
| Sensores e volante pela CAN | O `RePQProtocol` do Fox lê o radar traseiro (cmd 0x22), o dianteiro (0x23) e o ângulo do volante (0x26, `eps`, máximo 537). Isso chega no `ICanBusListener` pelos códigos 3 (radar) e 4 (volante). Também dá para ler direto com `ICanBus.getRadarInfo` (15) e `getSteerWheelInfo` (16). O `CanBusClient` do launcher hoje **ignora** esses dois. |

## Segurança

- Carro **parado, freio de mão puxado**. Chave na ignição; o motor pode ficar **desligado** (o Fox
  é manual, e a luz de ré funciona só com a ignição ligada).
- O teste do volante com o motor desligado deixa o volante pesado. Girar um pouco já basta para
  ver se as linhas se mexem.
- Não mudar nada nas telas de ajuste durante o teste, só olhar.

## Preparação (antes de sair de casa)

- [ ] `./gradlew assembleDebug` (o APK de debug traz a sonda da ré). No começo, o script pergunta se deve
      instalá-lo na central (`adb install -r`, que mantém os dados e a escolha de HOME).
- [ ] Notebook com o projeto e o cabo/adb que você já usa para a central.
- [ ] Celular para fotografar a tela. A captura do Android pode sair preta onde fica o vídeo da câmera.
- [ ] Um pendrive ou celular com música, para tocar durante a ré.

## Roteiro

Tudo menos as anotações é feito pelo script. Ele para e pede cada ação:

```powershell
powershell -ExecutionPolicy Bypass -File tools\re-check\coletar.ps1
```

A saída vai para `jancar-apks\re-check\<data_hora>\`, e os APKs para `jancar-apks\re-check\apks\`.

| # | O que o script faz | O que você faz e anota |
|---|---|---|
| 1 | Lê as propriedades, o usuário do adb e as montagens | — |
| 2 | Lista os apps de câmera/ré instalados e copia os detalhes de cada um | — |
| 3 | Copia os `.ini` (linhas, volume, telas de ajuste) e testa se dá para gravar em `/jancar/config` | — |
| 4 | Vê se a câmera aparece como câmera Android comum (`media.camera`, `/dev/video*`) | — |
| 5 | Copia os APKs da câmera e do Configurações | — |
| 6 | Salva o log desde que a central ligou | — |
| 7 | Tira uma foto da tela normal | — |
| 8 | Abre a câmera com a mão, **sem ré** | A imagem apareceu? (sim / tela preta / erro) |
| 8b | Abre a **sonda da ré** com a camada de teste ligada | Anotar "Volume na ré (15)": valor/mín./máx. |
| 9 | Grava log contínuo, janelas e fotos da tela **com a ré** | Música tocando → engatar a ré. A música abaixou? Quanto? **As linhas coloridas de teste aparecem por cima da câmera?** (foto com o celular). Tem linhas da central? Fixas? Mexem com o volante? O texto "volante:" e "sensores:" da camada muda? |
| 10 | Foto depois de sair da ré | Voltou para a tela de antes? |
| 10b | Volta para a sonda | Tocar "testar 100", engatar a ré de novo: a música ainda abaixa? Deixar em 100 ou "voltar ao original". |
| 11 | Abre o ajuste da ré (`BackCarSettingActivity`) | Quais opções aparecem. Depois abrir Configurações → Áudio: existe "volume na ré"? No fim, "Encerrar teste" na sonda. |
| 12 | Separa as linhas do log ligadas à ré e ao volume | — |

## Ficha de anotações

| Pergunta | Resposta |
|---|---|
| Qual app de ré está instalado? (passo 2) | |
| `/jancar/config` dá para gravar pelo adb? (passo 3) | |
| O `ivi-config.ini` tem `[car_ccd_line]`? Com quais valores? (passo 3) | |
| O `ivi-settings.ini` esconde `MediaCCDVolume`? (passo 3) | |
| A câmera aparece no `media.camera`? (passo 4) | |
| Abre na mão, sem ré? (passo 8) | |
| A música abaixa na ré? Quanto? (passo 9) | |
| Tem linhas? Fixas ou móveis? (passo 9) | |
| O carro tem sensor de estacionamento? Aparece na tela? (passo 9) | |
| Qual janela fica por cima na ré (pacote / tipo)? (passo 9, `09-janelas-re.txt`) | |
| A tela de ajuste da ré abre? Quais opções? (passo 11) | |
| Tem "volume na ré" no Áudio? (passo 11) | |
| Sonda: volume na ré valor/mín./máx. (passo 8b) | |
| Sonda: "Ré (ICar, fio)" muda ao engatar? Chegou algum "aviso"? (passo 9) | |
| Sonda: a camada de teste aparece por cima da câmera? (passo 9) | |
| Sonda: volante e sensores do CAN têm valor? Mudam? (passo 9) | |
| Com "testar 100", a música ainda abaixa? (passo 10b) | |

## O que cada resposta decide

- **Tem "volume na ré" visível:** basta colocar no máximo. Fim do pedido 1.
- **Não tem:** o launcher chama `IAudio.setParam(15, 100)` ao ligar. Ou, se `/jancar/config` for
  gravável, dá para mostrar o item no `ivi-settings.ini` ou mudar `CcdVolumePercentDefault` no
  `ivi-customer.ini`.
- **`/jancar/config` gravável e o `ivi-config.ini` tem `[car_ccd_line]`:** colocar as medidas do Fox
  no lugar das do Golf 7 e ajustar `TRACK_OFFSET_X/Y` (sempre com backup do arquivo original).
- **Não gravável:** desligar as linhas do sistema no ajuste da ré e desenhar as nossas por cima da
  câmera, com o overlay do launcher. Para isso, a janela da câmera (passo 9) precisa ficar abaixo de
  um `TYPE_APPLICATION_OVERLAY`.
- **APK da câmera copiado:** descompilar (pipeline do vdex010) para ver como as linhas são
  desenhadas (`disp_style`, `pts`) e se a ré rápida do boot usa imagens fixas (`OnlySaticTrackByImg`).
- **Linhas não se mexem com o volante:** provavelmente o Fox não manda o ângulo do volante (é preciso
  ter sensor de ângulo, comum só com ESP). Só a sonda (Parte B) confirma.

## Parte B: sonda da ré (pronta em 2026-11-15, testada só no emulador)

O que o adb sozinho não mostra fica na sonda, uma tela que só existe no build debug:
`ReverseProbeActivity` + `ReverseProbe` (`app/src/debug`). Abrir com
`adb shell am start -n com.rocha.jancarlauncher/.ReverseProbeActivity [--ez overlay true]`.
Ela loga tudo com a etiqueta `ReProbe`.

- **Volume na ré:** liga no `IAudio` e lê o param 15 (valor, mín., máx., padrão). "Testar 100" chama
  `setParam(15, 100)`, a **única escrita** da sonda. "Voltar ao original" usa o primeiro valor lido, que
  fica salvo no app.
- **Ré:** `CarState.reverseOn` (`ICar.getCcdStatus`, código 10, e o aviso `onCcdChanged`, código 3; agora
  no `JancarCarClient`, só leitura). Também escuta os broadcasts `android.backcar.action.*`,
  `action_backcar_notification` e `com.zjinnova.zlink.action.BACKCAR_*`.
- **CAN:** o `CanBusClient` agora lê radar (aviso 3, `getRadarInfo` 15) e volante (aviso 4,
  `getSteerWheelInfo` 16), e loga uma vez cada código de aviso que chega sem ser lido. Enquanto roda, a
  sonda segura a conexão contada.
- **Camada por cima:** `TYPE_APPLICATION_OVERLAY` de tela cheia que não recebe toque, com linhas verde,
  amarela e vermelha (curvam com o ângulo do volante, se vier) e os valores ao vivo. Ela continua na
  tela com a sonda atrás da câmera. "Encerrar teste" tira a camada e desliga tudo.

Testado no emulador: a camada aparece, continua por cima de outro app (HOME) e some ao encerrar.
Sem os serviços da Jancar, tudo fica "sem dado" e o `IAudio` não conecta. Ainda não foi testada
**na central** nem por cima da câmera.

Não coberto: se o valor 100 continua depois de reiniciar a central. Para ver, reinicie e abra a sonda de
novo (a linha "Volume na ré (15)").
