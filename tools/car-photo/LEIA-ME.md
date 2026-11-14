# Foto do carro (tela do Carro)

A tela do Carro mostra a **foto real do Fox do dono** (`app/src/main/assets/car/photo.png`), recortada do fundo e
com a **placa em branco**. Por cima, o app desenha só o que vem dos sinais reais (`ui/car3d/CarPhoto.kt`):
brilho nos faróis (fio de iluminação), setas piscando (CAN) e destaque âmbar nas portas abertas que aparecem na
foto. As portas que não aparecem nesse ângulo ficam no texto da linha de baixo.

Decisão do dono (2026-11-13): o carro 3D gerado por código não ficava realista, e a câmera já era fixa; trocou-se
pela foto. O código e o modelo 3D foram removidos.

## A foto

- Original: `docs/Fox-photos/IMG_20261008_210115332_HDR_PCT.jpg.jpeg` (3/4 pela frente do lado do **motorista**,
  faróis apagados). Fica fora do git (tem a placa).
- Quadro de trabalho: a foto em pé (rotação do EXIF aplicada), cortada a partir de (40, 1380) e reduzida pela
  metade → 1400×740. O contorno e as posições do app estão nesse quadro.

## Refazer ou ajustar o recorte

1. Ver o contorno sobre a foto, ampliado:
   `powershell -File tools/car-photo/grade.ps1 0 360 260 360 3 janela.png ((Get-Content tools/car-photo/contorno.txt) -join ";")`
   (janela x 0, y 360, 260×360, ampliada 3×).
2. Ajustar os pontos em `contorno.txt` (um `x,y` por linha, sentido horário).
3. Gerar a foto do app: `powershell -File tools/car-photo/recortar.ps1` (opcional `-preview prévia.png` sobre
   xadrez). A placa em branco é desenhada pelo script.
4. Se o recorte mudar de origem/tamanho (o script imprime), atualizar `W`, `H`, `OX`, `OY` em `CarPhoto.kt`.

## Trocar a foto

Nova foto → novo quadro de trabalho, novo contorno, novo retângulo da placa (em `recortar.ps1`) e novas posições
em `CarPhoto.SPEC` (faróis, setas, contorno de cada porta visível). Lado da foto define quais setas/portas são
"de perto". Para conferir sem o carro: `CarPreviewActivity` (só no build debug), ex.
`adb shell am start -f 0x10008000 -n com.rocha.jancarlauncher/.CarPreviewActivity --ez lights true --es turn hazard --es open fl,rl`.

## Papel de parede da Home

`papel-de-parede.ps1` gera `app/src/main/res/drawable-nodpi/home_wallpaper.jpg` (1024×600) da mesma foto original: recorte na largura toda, centrado no carro, sem tratamento de cor, placa em branco (mesmo polígono do recorte, em coordenadas da foto original).

```
powershell -NoProfile -ExecutionPolicy Bypass -File tools/car-photo/papel-de-parede.ps1
```
