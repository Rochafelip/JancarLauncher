# Papel de parede da Home: a foto real do Fox (IMG_20261008_210115332), sem tratamento de cor nem desfoque, recortada
# na proporção da tela da central (1024x600) com o carro no meio e a placa em branco (decisão do dono).
# Uso: powershell -File tools/car-photo/papel-de-parede.ps1 [-out app/src/main/res/drawable-nodpi/home_wallpaper.jpg]
param([string]$out = (Join-Path $PSScriptRoot "../../app/src/main/res/drawable-nodpi/home_wallpaper.jpg"))
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile([System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../../docs/Fox-photos/IMG_20261008_210115332_HDR_PCT.jpg.jpeg")))
if ($src.PropertyIdList -contains 0x0112) { $o = $src.GetPropertyItem(0x0112).Value[0]; switch ($o) { 3 { $src.RotateFlip("Rotate180FlipNone") } 6 { $src.RotateFlip("Rotate90FlipNone") } 8 { $src.RotateFlip("Rotate270FlipNone") } } }

# Na foto em pé (3072x4096) o carro vai de y~1470 a y~2870: recorte da largura toda, centrado nele.
$W = 1024; $H = 600
$cropW = $src.Width; $cropH = [int]($cropW * $H / $W)
$carCenterY = 2172
$cropY = [math]::Max(0, [math]::Min($src.Height - $cropH, $carCenterY - [int]($cropH / 2)))

$bmp = New-Object System.Drawing.Bitmap $W, $H
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = 'HighQualityBicubic'; $g.SmoothingMode = 'AntiAlias'
$g.DrawImage($src, (New-Object System.Drawing.Rectangle 0, 0, $W, $H), (New-Object System.Drawing.Rectangle 0, $cropY, $cropW, $cropH), 'Pixel')

# Placa em branco: o mesmo polígono do recorte (recortar.ps1), em coordenadas da foto original.
$k = $W / $cropW
$plate = @(@(194,2370),@(406,2414),@(406,2542),@(190,2482)) | ForEach-Object {
  New-Object System.Drawing.PointF ([single]($_[0] * $k)), ([single](($_[1] - $cropY) * $k)) }
$g.FillPolygon((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,222,224,226))), $plate)
$g.DrawPolygon((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,80,82,86)), 1.2), $plate)
$g.Dispose(); $src.Dispose()

$enc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$ps = New-Object System.Drawing.Imaging.EncoderParameters 1
$ps.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), ([long]88)
$bmp.Save([System.IO.Path]::GetFullPath($out), $enc, $ps); $bmp.Dispose()
"ok: $out (recorte y=$cropY, ${cropW}x$cropH)"
