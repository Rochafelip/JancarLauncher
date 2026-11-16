# Foto do carro usada pela tela do Fox e o "quadro de trabalho" onde ficam as posições do app.
# Incluído (dot-source) por grade.ps1 e recortar.ps1.
#
# Foto atual (2026-11-15, pedido do dono: sem os reflexos das árvores): "Volkswagen Fox Prata Isolado em PNG.png",
# 3/4 pela frente do lado do motorista, luz uniforme, JÁ ISOLADA (fundo transparente no próprio PNG). Por isso
# $ISOLADA = $true: o recorte usa a transparência da foto e não o contorno.txt (que serve para foto com fundo).
# Quadro de trabalho = a foto como veio (1536x1024), sem corte, escala nem rotação.
$FOTO = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../../docs/Fox-photos/Volkswagen Fox Prata Isolado em PNG.png"))
$ROT = ""
$ISOLADA = $true
$OX = 0; $OY = 0; $S = 1.0
$QUADRO_W = 1536; $QUADRO_H = 1024   # (nomes próprios: o PowerShell não distingue $W de $w dos scripts)

# Placa (em branco no app; decisão do dono), no quadro de trabalho.
$PLACA = @(@(118,683),@(289,694),@(290,776),@(117,765))

function Open-Foto {
  Add-Type -AssemblyName System.Drawing
  $img = [System.Drawing.Image]::FromFile($FOTO)
  # Foto de celular grava "em pé" só na etiqueta EXIF; sem etiqueta, vale a rotação fixa acima.
  if ($img.PropertyIdList -contains 0x0112) {
    $o = $img.GetPropertyItem(0x0112).Value[0]
    switch ($o) { 3 { $img.RotateFlip("Rotate180FlipNone") } 6 { $img.RotateFlip("Rotate90FlipNone") } 8 { $img.RotateFlip("Rotate270FlipNone") } }
  } elseif ($ROT) { $img.RotateFlip($ROT) }
  return $img
}
