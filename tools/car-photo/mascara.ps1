# Máscara da lataria da foto do app (app/src/main/assets/car/photo.png) -> app/src/main/assets/car/body_mask.png,
# usada para trocar a cor do Fox por diversão (ui/car3d/CarPaint.kt). Mesmo tamanho da foto; alfa = lataria.
# Lataria = pixel do carro com pouca saturação (o Fox é prata) e luminância média/alta, MENOS os polígonos abaixo
# (vidros, rodas, faróis, grade, placa, farol de milha), que também são cinza/cromados mas não são pintura.
# Borda suavizada (desfoque 3x3). Regenerar sempre que a foto mudar.
# Uso: powershell -NoProfile -ExecutionPolicy Bypass -File tools/car-photo/mascara.ps1 [-preview prévia.png]
param(
  [string]$photo = (Join-Path $PSScriptRoot "../../app/src/main/assets/car/photo.png"),
  [string]$out = (Join-Path $PSScriptRoot "../../app/src/main/assets/car/body_mask.png"),
  [string]$preview = ""
)
Add-Type -AssemblyName System.Drawing

$SAT_MAX = 0.25   # acima disto é cor (lanterna, reflexo de árvore saturado): não é lataria
$LUM_MIN = 0.25   # abaixo: plástico preto, pneu, sombra funda
$LUM_MAX = 1.00   # reflexo do céu na pintura também é pintura (fica claro na cor nova)

# Polígonos fora da lataria, em pixels da photo.png (1228x690). Medir com grade.ps1 se a foto mudar.
$FORA = @(
  # para-brisa
  @(@(285,214),@(360,152),@(445,100),@(535,68),@(600,60),@(772,56),@(732,140),@(674,212),@(560,226),@(400,226)),
  # janelas laterais (sem pegar a capa do retrovisor, que é pintada)
  @(@(782,106),@(940,40),@(1076,50),@(1097,66),@(1119,156),@(990,202),@(905,213),@(902,178),@(800,178),@(784,160)),
  # farol direito (o grande) e a pontinha do farol esquerdo
  @(@(318,442),@(338,400),@(420,366),@(540,340),@(570,348),@(556,402),@(470,430),@(380,447)),
  @(@(16,372),@(30,342),@(50,362),@(50,432),@(24,428)),
  # grade cromada + emblema
  @(@(44,383),@(150,382),@(338,396),@(334,442),@(150,448),@(46,437)),
  # placa (já em branco)
  @(@(88,474),@(236,480),@(234,554),@(88,549))
)
# Elipses fora da lataria: x, y, largura, altura (rodas de liga e farol de milha).
$ELIPSES = @(
  @(596,436,172,222),   # roda dianteira
  @(1132,342,96,156),   # roda traseira
  @(452,516,50,50)      # farol de milha
)

function Get-Bytes($bmp) {
  $r = New-Object System.Drawing.Rectangle 0, 0, $bmp.Width, $bmp.Height
  $d = $bmp.LockBits($r, 'ReadOnly', [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $n = $bmp.Width * $bmp.Height * 4; $b = New-Object byte[] $n
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $b, 0, $n); $bmp.UnlockBits($d); return ,$b
}
function Set-Bytes($bmp, $b) {
  $r = New-Object System.Drawing.Rectangle 0, 0, $bmp.Width, $bmp.Height
  $d = $bmp.LockBits($r, 'WriteOnly', [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  [System.Runtime.InteropServices.Marshal]::Copy($b, 0, $d.Scan0, $b.Length); $bmp.UnlockBits($d)
}

$src0 = [System.Drawing.Bitmap]::FromFile((Resolve-Path $photo))
$W = $src0.Width; $H = $src0.Height
$src = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($src); $g.DrawImage($src0, 0, 0, $W, $H); $g.Dispose(); $src0.Dispose()

# Camada "fora": branco onde NÃO é lataria (polígonos e elipses), com borda suave.
$layer = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gf = [System.Drawing.Graphics]::FromImage($layer); $gf.SmoothingMode = 'AntiAlias'; $gf.Clear([System.Drawing.Color]::Black)
$white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
foreach ($poly in $FORA) {
  $pts = New-Object "System.Drawing.PointF[]" $poly.Count
  for ($k = 0; $k -lt $poly.Count; $k++) { $pts[$k] = New-Object System.Drawing.PointF ([single]$poly[$k][0]), ([single]$poly[$k][1]) }
  $gf.FillPolygon($white, $pts)
}
foreach ($e in $ELIPSES) { $gf.FillEllipse($white, [single]$e[0], [single]$e[1], [single]$e[2], [single]$e[3]) }
$gf.Dispose()

$bs = Get-Bytes $src
$bf = Get-Bytes $layer
$n = $W * $H
$m = New-Object byte[] $n
for ($p = 0; $p -lt $n; $p++) {
  $i = $p * 4
  $a = $bs[$i + 3]
  if ($a -lt 128 -or $bf[$i + 2] -gt 127) { continue }
  $b = $bs[$i]; $gg = $bs[$i + 1]; $r = $bs[$i + 2]
  $mx = [math]::Max($r, [math]::Max($gg, $b)); $mn = [math]::Min($r, [math]::Min($gg, $b))
  if ($mx -eq 0) { continue }
  $sat = ($mx - $mn) / $mx
  $lum = (0.299 * $r + 0.587 * $gg + 0.114 * $b) / 255.0
  if ($sat -le $SAT_MAX -and $lum -ge $LUM_MIN -and $lum -le $LUM_MAX) { $m[$p] = 255 }
}

# Desfoque 3x3 (borda suave) e saída: branco com alfa = máscara.
$outB = New-Object byte[] ($n * 4)
for ($y = 0; $y -lt $H; $y++) {
  for ($x = 0; $x -lt $W; $x++) {
    $s = 0; $c = 0
    for ($dy = -1; $dy -le 1; $dy++) {
      $yy = $y + $dy; if ($yy -lt 0 -or $yy -ge $H) { continue }
      for ($dx = -1; $dx -le 1; $dx++) {
        $xx = $x + $dx; if ($xx -lt 0 -or $xx -ge $W) { continue }
        $s += $m[$yy * $W + $xx]; $c++
      }
    }
    $o = ($y * $W + $x) * 4
    $outB[$o] = 255; $outB[$o + 1] = 255; $outB[$o + 2] = 255; $outB[$o + 3] = [byte]([math]::Round($s / $c))
  }
}
$mask = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Set-Bytes $mask $outB
$mask.Save((Join-Path (Split-Path $out) (Split-Path $out -Leaf)), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "mascara: $out (${W}x${H})"

if ($preview) {
  # Prévia: xadrez cinza, a foto por cima e a lataria pintada de vermelho (multiplicação) para conferir o recorte.
  $pv = Get-Bytes $src
  for ($p = 0; $p -lt $n; $p++) {
    $i = $p * 4; $k = $outB[$i + 3] / 255.0
    if ($k -gt 0) {
      $pv[$i] = [byte]($pv[$i] * (1 - 0.85 * $k)); $pv[$i + 1] = [byte]($pv[$i + 1] * (1 - 0.85 * $k))
    }
  }
  $pb = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  Set-Bytes $pb $pv
  $canvas = New-Object System.Drawing.Bitmap $W, $H
  $gc = [System.Drawing.Graphics]::FromImage($canvas)
  for ($y = 0; $y -lt $H; $y += 20) { for ($x = 0; $x -lt $W; $x += 20) {
    $col = if ((($x + $y) / 20) % 2 -eq 0) { [System.Drawing.Color]::FromArgb(200,200,200) } else { [System.Drawing.Color]::FromArgb(150,150,150) }
    $gc.FillRectangle((New-Object System.Drawing.SolidBrush $col), $x, $y, 20, 20)
  } }
  $gc.DrawImage($pb, 0, 0, $W, $H); $gc.Dispose()
  $canvas.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
  Write-Host "previa: $preview"
}
