# Carro 3D — contrato do modelo (.glb)

O app desenha o arquivo `app/src/main/assets/car/fox.glb`. Trocar esse arquivo troca o carro, **sem mexer no
código**, desde que o modelo siga as regras abaixo. Hoje o arquivo é a maquete provisória gerada por
`tools/car3d/make-placeholder-fox.mjs` (a tela mostra "MODELO PROVISÓRIO" enquanto o arquivo tiver
`asset.extras.placeholder = true`).

## Por que essas regras

A central é uma **Mali-450 com OpenGL ES 2.0** e tem ~185 MB de RAM livre. Filament/SceneView exigem ES 3.0
e não rodam lá, por isso o app tem um renderizador próprio, enxuto (`ui/car3d/CarRenderer.kt`), que lê só o
básico do glTF.

## Formato e orçamento

| Item | Regra |
| --- | --- |
| Arquivo | glTF 2.0 **binário** (`.glb`), tudo embutido (sem texturas externas) |
| Unidades / eixos | metros; **+Y para cima, frente do carro em +Z**, lado do motorista (esquerdo) em **+X**; origem no chão, no meio do carro (padrão do exportador do Blender com "+Y Up") |
| Triângulos | até ~60 mil no carro inteiro (o provisório tem 1,8 mil) |
| Vértices | até 65 535 **por primitiva** (índice de 16 bits; acima disso o app desindexa e gasta mais memória) |
| Texturas | JPG/PNG, até **1024 px** (o app reduz se vier maior); lado em potência de 2 para ter mipmap |
| Materiais | PBR básico: cor (`baseColorFactor` × `baseColorTexture`), `metallicFactor`, `roughnessFactor`, `emissiveFactor`, `doubleSided`, `alphaMode` OPAQUE ou BLEND. Normal map, oclusão e extensões `KHR_*` são ignorados |
| Não usar | compressão Draco/meshopt, atributos quantizados, skin, morph, animações do arquivo |

Sombras e brilhos finos ficam melhores "assados" na textura de cor (o shader é simples de propósito).

## Nós com nome fixo

O app procura estes nomes **exatos**. Cada peça móvel precisa estar num nó próprio com o **pivô (origem do
nó) na dobradiça**; a malha fica parada em relação ao pivô.

| Nó | O que faz | Giro (em torno do eixo local do nó) |
| --- | --- | --- |
| `door_fl` | porta do motorista | Y, −62° (abre para fora) |
| `door_fr` | porta do passageiro | Y, +62° |
| `door_rl` | traseira esquerda | Y, −62° |
| `door_rr` | traseira direita | Y, +62° |
| `tailgate` | tampa traseira | X, +78° (dobradiça em cima) |
| `hood` | capô | X, −42° (dobradiça perto do para-brisa; hoje o CAN do Fox não manda o capô, fica sempre fechado) |
| `lamp_head_l`, `lamp_head_r` | faróis (acendem com o fio de iluminação) | — |
| `lamp_tail_l`, `lamp_tail_r` | lanternas (acendem junto com o farol) | — |
| `lamp_turn_fl`, `lamp_turn_rl` | setas esquerdas (piscam) | — |
| `lamp_turn_fr`, `lamp_turn_rr` | setas direitas (piscam) | — |
| `lamp_turn_ml`, `lamp_turn_mr` | repetidor de seta no retrovisor esq. / dir. (opcional; **filho do nó da porta** dianteira) | — |

- Ângulos e eixos ficam em `ui/car3d/CarRig.kt`. Se o modelo comprado vier com o eixo da dobradiça diferente,
  dá pra ajustar lá em vez de mexer no modelo.
- As lanternas acendem pelo **nó**: tudo que estiver na malha do nó `lamp_*` ganha a cor de luz. Então o nó deve
  conter só a lente (não a carcaça).
- Retrovisor, maçaneta e vidro da porta devem ser **filhos** do nó da porta (para abrirem junto). O aerofólio
  (com a luz de freio alta, que fica apagada: não há sinal do freio) é parte da tampa traseira.
- A pintura da lataria ganha verniz (reflexo nítido por cima) quando o nome do material tem `paint`, `body` ou
  `lataria`; os outros materiais ficam sem.
- Rodas (`wheel_fl` etc.) existem no provisório, mas o app não as gira (não há fonte confiável de velocidade
  por roda; não simular).

## Preparar o modelo comprado (Blender)

1. Conferir com as fotos (`docs/Fox-photos/`, lateral/traseira/farol aceso de 2026-11-13):
   - Fox G2 4 portas, prata, vidros todos com película bem escura; colunas B e triângulo do retrovisor pretos.
   - Vidro da porta traseira dividido por um filete preto (parte fixa no fundo da porta).
   - Frisos laterais finos na cor do carro nas portas; portinhola de combustível na traseira direita.
   - Retrovisores na cor do carro com **repetidor de seta** embutido (deve piscar junto com a seta).
   - Frente: grade preta de barras horizontais com frisos cromados, emblema VW; faróis halógenos com máscara
     escura (sem faixa de LED), seta âmbar embaixo do farol; para-choque com colmeia preta e faróis de neblina
     redondos em moldura preta.
   - Traseira: aerofólio no teto com **brake light** (luz de freio alta), limpador traseiro, lanternas G2
     (vermelho por fora, parte clara/fumê por dentro), sensores de ré no para-choque, engate com bola, refletor
     vermelho baixo no para-choque, **câmera de ré** (ponto preto logo abaixo do emblema VW da tampa —
     confirmado pelo dono).
   - Teto: antena "barbatana" preta.
   - Rodas de liga 15" prata, 5 pares de raios em V, calota central VW.
   - Placa em branco (decisão do dono: sem os caracteres reais no modelo).
2. Separar portas, tampa, capô e lentes das lanternas em objetos próprios e pôr a origem de cada um na
   dobradiça (`Set Origin > Origin to 3D Cursor`, com o cursor na dobradiça).
3. Renomear os objetos conforme a tabela.
4. Reduzir polígonos (Decimate) até o orçamento; texturas a 1024 px.
5. Exportar: glTF Binary (.glb), +Y Up, aplicar modificadores, sem Draco, sem animação.
6. Salvar como `app/src/main/assets/car/fox.glb` e conferir na prévia:
   `adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.CarPreviewActivity --es open fl,fr,rl,rr,trunk --ez lights true --es turn hazard`

## Na central (a medir)

- Quadros por segundo girando o carro e memória (`adb shell dumpsys meminfo com.rocha.jancarlauncher`):
  meta do projeto é PSS ≤ ~70 MB.
- O desenho só roda quando algo muda (toque, porta se mexendo, seta piscando); parado não gasta GPU.
