# Ajuda a traçar/ajustar o contorno: mostra uma janela do quadro de trabalho (a foto reduzida pela metade,
# 1400x740) ampliada [k]x, com grade a cada 10 px (rótulos a cada 50) e o contorno [poly] em vermelho.
# Ex.: powershell -File tools/car-photo/grade.ps1 0 360 260 360 3 janela.png ((Get-Content tools/car-photo/contorno.txt) -join ";")
param([int]$x0, [int]$y0, [int]$w, [int]$h, [double]$k, [string]$out, [string]$poly)
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile([System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../../docs/Fox-photos/IMG_20261008_210115332_HDR_PCT.jpg.jpeg")))
# Aplica a rotação do EXIF (o celular grava em pé só na etiqueta).
if ($src.PropertyIdList -contains 0x0112) { $o = $src.GetPropertyItem(0x0112).Value[0]; switch ($o) { 3 { $src.RotateFlip("Rotate180FlipNone") } 6 { $src.RotateFlip("Rotate90FlipNone") } 8 { $src.RotateFlip("Rotate270FlipNone") } } }
$OX = 40; $OY = 1380; $S = 0.5   # quadro de trabalho 1400x740 = original (x 40.., y 1380..) × 0,5
$bmp = New-Object System.Drawing.Bitmap ([int]($w*$k)), ([int]($h*$k))
$g = [System.Drawing.Graphics]::FromImage($bmp); $g.InterpolationMode = 'HighQualityBicubic'
$sr = New-Object System.Drawing.RectangleF ([single]($OX + $x0/$S)), ([single]($OY + $y0/$S)), ([single]($w/$S)), ([single]($h/$S))
$g.DrawImage($src, (New-Object System.Drawing.RectangleF 0,0,$bmp.Width,$bmp.Height), $sr, 'Pixel')
$thin = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(70,255,0,255)), 1
$thick = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(150,0,255,255)), 1
$font = New-Object System.Drawing.Font "Arial", 10
$br = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::Yellow)
$step = if ($k -ge 2) { 10 } else { 25 }
for ($x = [math]::Ceiling($x0/$step)*$step; $x -lt $x0+$w; $x += $step) { $px = ($x-$x0)*$k; $p = if ($x % 50 -eq 0) { $thick } else { $thin }; $g.DrawLine($p, $px, 0, $px, $bmp.Height); if ($x % 50 -eq 0) { $g.DrawString("$x", $font, $br, $px+2, 2) } }
for ($y = [math]::Ceiling($y0/$step)*$step; $y -lt $y0+$h; $y += $step) { $py = ($y-$y0)*$k; $p = if ($y % 50 -eq 0) { $thick } else { $thin }; $g.DrawLine($p, 0, $py, $bmp.Width, $py); if ($y % 50 -eq 0) { $g.DrawString("$y", $font, $br, 2, $py+2) } }
if ($poly) {
  $pts = $poly.Split(';') | ForEach-Object { $a = $_.Split(','); New-Object System.Drawing.PointF ([single](([double]$a[0]-$x0)*$k)), ([single](([double]$a[1]-$y0)*$k)) }
  $g.DrawPolygon((New-Object System.Drawing.Pen ([System.Drawing.Color]::Red), 2), $pts)
}
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose(); $src.Dispose()
