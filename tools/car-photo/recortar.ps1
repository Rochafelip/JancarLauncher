# Recorte da foto definitiva (IMG_20261008_210115332): contorno do carro -> PNG com transparência, placa em branco,
# cortado no tamanho do carro. Coordenadas no quadro de trabalho 1400x740 (original x 40.., y 1380.., × 0,5).
# Uso: powershell -File tools/car-photo/recortar.ps1 [-out app/src/main/assets/car/photo.png] [-preview recorte.png]
# O contorno vem de tools/car-photo/contorno.txt (um ponto "x,y" por linha, no quadro de trabalho).
param([string]$out = (Join-Path $PSScriptRoot "../../app/src/main/assets/car/photo.png"), [string]$preview = "", [string]$polyText = "")
if (-not $polyText) { $polyText = ((Get-Content (Join-Path $PSScriptRoot "contorno.txt")) | Where-Object { $_.Trim() }) -join ";" }
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile([System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../../docs/Fox-photos/IMG_20261008_210115332_HDR_PCT.jpg.jpeg")))
if ($src.PropertyIdList -contains 0x0112) { $o = $src.GetPropertyItem(0x0112).Value[0]; switch ($o) { 3 { $src.RotateFlip("Rotate180FlipNone") } 6 { $src.RotateFlip("Rotate90FlipNone") } 8 { $src.RotateFlip("Rotate270FlipNone") } } }
$OX = 40; $OY = 1380; $S = 0.5; $W = 1400; $H = 740
$pts = $polyText.Split(';') | ForEach-Object { $a = $_.Split(','); New-Object System.Drawing.PointF ([single][double]$a[0]), ([single][double]$a[1]) }
$minX = [int]([math]::Floor(($pts | Measure-Object X -Minimum).Minimum)) - 4; $maxX = [int]([math]::Ceiling(($pts | Measure-Object X -Maximum).Maximum)) + 4
$minY = [int]([math]::Floor(($pts | Measure-Object Y -Minimum).Minimum)) - 4; $maxY = [int]([math]::Ceiling(($pts | Measure-Object Y -Maximum).Maximum)) + 4
$minX = [math]::Max(0, $minX); $minY = [math]::Max(0, $minY); $maxX = [math]::Min($W, $maxX); $maxY = [math]::Min($H, $maxY)
$w = $maxX - $minX; $h = $maxY - $minY
$local = $pts | ForEach-Object { New-Object System.Drawing.PointF ($_.X - $minX), ($_.Y - $minY) }

$img = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gi = [System.Drawing.Graphics]::FromImage($img); $gi.InterpolationMode = 'HighQualityBicubic'
$sr = New-Object System.Drawing.RectangleF ([single]($OX + $minX/$S)), ([single]($OY + $minY/$S)), ([single]($w/$S)), ([single]($h/$S))
$gi.DrawImage($src, (New-Object System.Drawing.RectangleF 0,0,$w,$h), $sr, 'Pixel')
# Placa em branco (decisão do dono).
$gi.SmoothingMode = 'AntiAlias'
$plate = @(@(77,495),@(183,517),@(183,581),@(75,551)) | ForEach-Object { New-Object System.Drawing.PointF ([single]($_[0]-$minX)), ([single]($_[1]-$minY)) }
$gi.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,222,224,226))), $plate)
$gi.DrawPolygon((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,80,82,86)), 1.5), $plate)
$gi.Dispose()

# Máscara com borda suavizada.
$mask = New-Object System.Drawing.Bitmap $w, $h
$gm = [System.Drawing.Graphics]::FromImage($mask); $gm.SmoothingMode = 'AntiAlias'; $gm.Clear([System.Drawing.Color]::Black)
$gm.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)), $local); $gm.Dispose()

# Junta máscara e foto direto na memória (rápido).
$rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
$di = $img.LockBits($rect, 'ReadWrite', [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$dm = $mask.LockBits($rect, 'ReadOnly', [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$n = $w * $h * 4
$bi = New-Object byte[] $n; $bm = New-Object byte[] $n
[System.Runtime.InteropServices.Marshal]::Copy($di.Scan0, $bi, 0, $n); [System.Runtime.InteropServices.Marshal]::Copy($dm.Scan0, $bm, 0, $n)
for ($i = 0; $i -lt $n; $i += 4) { $bi[$i + 3] = $bm[$i + 2] }
[System.Runtime.InteropServices.Marshal]::Copy($bi, 0, $di.Scan0, $n)
$img.UnlockBits($di); $mask.UnlockBits($dm)
$img.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

$pv = New-Object System.Drawing.Bitmap $w, $h
$gp = [System.Drawing.Graphics]::FromImage($pv)
for ($y = 0; $y -lt $h; $y += 20) { for ($x = 0; $x -lt $w; $x += 20) {
  $c = if ((($x + $y) / 20) % 2 -eq 0) { [System.Drawing.Color]::FromArgb(255,60,60,70) } else { [System.Drawing.Color]::FromArgb(255,30,30,36) }
  $gp.FillRectangle((New-Object System.Drawing.SolidBrush $c), $x, $y, 20, 20) } }
$gp.DrawImage($img, 0, 0); if ($preview) { $pv.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png) }
$gp.Dispose(); $pv.Dispose(); $img.Dispose(); $mask.Dispose(); $src.Dispose()
"recorte ${w}x${h}, origem ($minX, $minY)"
