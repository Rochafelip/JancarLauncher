# Foto do carro (tela do Carro)

A tela do Carro mostra a **foto real do Fox do dono** (`app/src/main/assets/car/photo.png`), recortada do fundo e
com a **placa em branco**. Por cima, o app desenha só o que vem dos sinais reais (`ui/car3d/CarPhoto.kt`):
brilho nos faróis e no farol de milha, setas piscando (CAN) e um balão com o nome de cada porta aberta, apontando
para um ponto na porta (as do passageiro, escondidas nesse ângulo, apontam para o teto do lado de lá).

Decisão do dono (2026-11-13): o carro 3D gerado por código não ficava realista, e a câmera já era fixa; trocou-se
pela foto. O código e o modelo 3D foram removidos.

## A foto

- Atual (2026-11-15, pedido do dono: sem os reflexos das árvores na lataria): `docs/Fox-photos/Volkswagen Fox
  Prata Isolado em PNG.png` — 3/4 pela frente do lado do **motorista**, faróis apagados, luz uniforme, **fundo já
  transparente** no próprio PNG. Fica fora do git (tem a placa).
- Tudo sobre a foto (arquivo, rotação, quadro de trabalho, placa) fica em `foto.ps1`. Quadro de trabalho atual =
  a foto como veio (1536×1024). As posições do app (`CarPhoto.SPEC`) estão nesse quadro.
- Antes (até 2026-11-15): `IMG_20261008_210115332_HDR_PCT.jpg.jpeg`, com fundo, recortada pelo `contorno.txt`
  (que ainda é o contorno dessa foto antiga, no quadro dela: em pé, a partir de (40, 1380), metade do tamanho).

## Gerar a foto do app

`powershell -File tools/car-photo/recortar.ps1` (opcional `-preview prévia.png` sobre xadrez). Foto isolada
(`$ISOLADA = $true`): usa a transparência do PNG e deixa o carro 100% opaco; foto com fundo: usa o `contorno.txt`.
Pinta a placa em branco, corta no carro e grava reduzida a 80% (menos memória na central). Se o recorte mudar de
origem/tamanho (o script imprime), atualizar `W`, `H`, `OX`, `OY` em `CarPhoto.kt`.

Para medir posições (ou ajustar um contorno): `grade.ps1` mostra uma janela do quadro de trabalho ampliada, com
grade (e o contorno em vermelho, se passado), ex.
`powershell -File tools/car-photo/grade.ps1 0 480 720 380 1.3 janela.png` (janela x 0, y 480, 720×380, 1,3×).

## Trocar a foto

Nova foto → `foto.ps1` (arquivo, rotação, isolada ou não, tamanho, placa), contorno novo se tiver fundo, e novas
posições em `CarPhoto.SPEC` (faróis, milha, setas e o ponto de cada porta para o balão). Lado da foto define quais
setas/portas são "de perto". Para conferir sem o carro: `CarPreviewActivity` (só no build debug), ex.
`adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.CarPreviewActivity --ez lights true --es turn hazard --es open fl,rl`.

## Papel de parede da Home

`papel-de-parede.ps1` gera `app/src/main/res/drawable-nodpi/home_wallpaper.jpg` (1024×600) da mesma foto original: recorte na largura toda, centrado no carro, sem tratamento de cor, placa em branco (mesmo polígono do recorte, em coordenadas da foto original).

```
powershell -NoProfile -ExecutionPolicy Bypass -File tools/car-photo/papel-de-parede.ps1
```
