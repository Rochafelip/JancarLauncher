# Recorte da foto do carro (ver foto.ps1) -> PNG com transparência, placa em branco, cortado no tamanho do carro.
# - Foto já isolada ($ISOLADA): usa a transparência do próprio PNG (o carro fica 100% opaco; borda suave mantida).
# - Foto com fundo: usa o contorno de tools/car-photo/contorno.txt (um ponto "x,y" por linha, no quadro de trabalho).
# A saída é reduzida por $SAIDA (a tela mostra a foto em ~290 dp; menos memória na central). As posições do app
# (CarPhoto.kt) ficam no quadro de trabalho: W/H/OX/OY = recorte impresso no fim.
# Uso: powershell -File tools/car-photo/recortar.ps1 [-out app/src/main/assets/car/photo.png] [-preview recorte.png]
param([string]$out = (Join-Path $PSScriptRoot "../../app/src/main/assets/car/photo.png"), [string]$preview = "", [string]$polyText = "")
. (Join-Path $PSScriptRoot "foto.ps1")
$SAIDA = 0.8
$src = Open-Foto
$FW = $QUADRO_W; $FH = $QUADRO_H

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

# Quadro de trabalho inteiro em ARGB.
$frame = New-Object System.Drawing.Bitmap $FW, $FH, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gf = [System.Drawing.Graphics]::FromImage($frame); $gf.InterpolationMode = 'HighQualityBicubic'
$gf.DrawImage($src, (New-Object System.Drawing.RectangleF 0, 0, $FW, $FH),
  (New-Object System.Drawing.RectangleF ([single]$OX), ([single]$OY), ([single]($FW/$S)), ([single]($FH/$S))), 'Pixel')
$gf.Dispose()

if (-not $ISOLADA) {
  # Máscara do contorno (borda suavizada) vira a transparência.
  if (-not $polyText) { $polyText = ((Get-Content (Join-Path $PSScriptRoot "contorno.txt")) | Where-Object { $_.Trim() }) -join ";" }
  $pts = $polyText.Split(';') | ForEach-Object { $a = $_.Split(','); New-Object System.Drawing.PointF ([single][double]$a[0]), ([single][double]$a[1]) }
  $mask = New-Object System.Drawing.Bitmap $FW, $FH, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gm = [System.Drawing.Graphics]::FromImage($mask); $gm.SmoothingMode = 'AntiAlias'; $gm.Clear([System.Drawing.Color]::Black)
  $gm.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), $pts); $gm.Dispose()
  $bi = Get-Bytes $frame; $bm = Get-Bytes $mask
  for ($i = 0; $i -lt $bi.Length; $i += 4) { $bi[$i + 3] = $bm[$i + 2] }
  Set-Bytes $frame $bi; $mask.Dispose()
}

# Transparência: a foto isolada veio com o carro em ~99% (alfa 252); o miolo fica 100% e a borda suave continua.
$bi = Get-Bytes $frame
$minX = $FW; $minY = $FH; $maxX = 0; $maxY = 0
for ($i = 0; $i -lt $bi.Length; $i += 4) {
  $a = $bi[$i + 3]
  if ($a -ge 236) { $bi[$i + 3] = 255 } elseif ($a -gt 0) { $bi[$i + 3] = [byte][math]::Min(255, [math]::Round($a * 255.0 / 236.0)) }
  if ($bi[$i + 3] -gt 16) {
    $p = $i / 4; $x = $p % $FW; $y = [math]::Floor($p / $FW)
    if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }; if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
  }
}
Set-Bytes $frame $bi

# Placa em branco (decisão do dono).
$gp = [System.Drawing.Graphics]::FromImage($frame); $gp.SmoothingMode = 'AntiAlias'
$plate = $PLACA | ForEach-Object { New-Object System.Drawing.PointF ([single]$_[0]), ([single]$_[1]) }
$gp.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,222,224,226))), $plate)
$gp.DrawPolygon((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,80,82,86)), 2), $plate)
$gp.Dispose()

# Corta no carro (margem de 4 px) e reduz.
$minX = [math]::Max(0, $minX - 4); $minY = [math]::Max(0, $minY - 4)
$maxX = [math]::Min($FW, $maxX + 5); $maxY = [math]::Min($FH, $maxY + 5)
$cw = $maxX - $minX; $ch = $maxY - $minY
$ow = [int][math]::Round($cw * $SAIDA); $oh = [int][math]::Round($ch * $SAIDA)
$img = New-Object System.Drawing.Bitmap $ow, $oh, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gi = [System.Drawing.Graphics]::FromImage($img); $gi.InterpolationMode = 'HighQualityBicubic'; $gi.PixelOffsetMode = 'HighQuality'
$gi.CompositingMode = 'SourceCopy'
$gi.DrawImage($frame, (New-Object System.Drawing.Rectangle 0, 0, $ow, $oh), (New-Object System.Drawing.Rectangle $minX, $minY, $cw, $ch), 'Pixel')
$gi.Dispose()
$img.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

if ($preview) {
  $pv = New-Object System.Drawing.Bitmap $ow, $oh
  $gv = [System.Drawing.Graphics]::FromImage($pv)
  for ($y = 0; $y -lt $oh; $y += 20) { for ($x = 0; $x -lt $ow; $x += 20) {
    $c = if ((($x + $y) / 20) % 2 -eq 0) { [System.Drawing.Color]::FromArgb(255,60,60,70) } else { [System.Drawing.Color]::FromArgb(255,30,30,36) }
    $gv.FillRectangle((New-Object System.Drawing.SolidBrush $c), $x, $y, 20, 20) } }
  $gv.DrawImage($img, 0, 0); $pv.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png); $gv.Dispose(); $pv.Dispose()
}
$img.Dispose(); $frame.Dispose(); $src.Dispose()
"recorte ${cw}x${ch} no quadro, origem ($minX, $minY); arquivo ${ow}x${oh}"
