# Coleta tudo sobre a camera de re da central pelo adb e salva numa pasta com data/hora.
# Uso (no PowerShell, com a central ligada no adb):
#   powershell -ExecutionPolicy Bypass -File tools\re-check\coletar.ps1
# O script para e pede para engatar/desengatar a re. Nada e gravado na central, exceto um
# arquivo de teste vazio em /jancar/config (criado e apagado na hora) para saber se da para gravar ali.
# Plano completo: docs/superpowers/plans/2026-11-16-verificacao-re-plan.md

$ErrorActionPreference = 'Continue'
$ADB = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$BASE = Join-Path $env:USERPROFILE 'AndroidStudioProjects\jancar-apks\re-check'
$OUT = Join-Path $BASE (Get-Date -Format 'yyyy-MM-dd_HH-mm')
New-Item -ItemType Directory -Force $OUT | Out-Null

function Sh([string]$nome, [string]$cmd) {
  $txt = (& $ADB shell $cmd 2>&1 | Out-String)
  Set-Content -Path (Join-Path $OUT "$nome.txt") -Value "# adb shell $cmd`r`n$txt" -Encoding UTF8
  Write-Host "  ok  $nome"
}
function Passo([string]$msg) { Write-Host ""; Write-Host "=== $msg ===" -ForegroundColor Cyan }
function Espera([string]$msg) { Write-Host ""; Write-Host ">>> $msg" -ForegroundColor Yellow; Read-Host "    (Enter quando pronto)" | Out-Null }
function Tela([string]$nome) {
  & $ADB shell screencap -p /sdcard/re_tela.png 2>&1 | Out-Null
  & $ADB pull /sdcard/re_tela.png (Join-Path $OUT "$nome.png") 2>&1 | Out-Null
  & $ADB shell rm /sdcard/re_tela.png 2>&1 | Out-Null
  Write-Host "  ok  $nome.png"
}

Passo "0. Conexao"
$dev = (& $ADB devices | Select-String -Pattern "\tdevice$")
if (-not $dev) { Write-Host "Nenhuma central no adb. Conecte e rode de novo." -ForegroundColor Red; exit 1 }
Write-Host "  central: $dev"
Write-Host "  saida:   $OUT"

# Launcher com a sonda da re (build debug). Reinstalar mantem dados, permissoes e a escolha de HOME.
$APK = Join-Path $PSScriptRoot '..\..\app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $APK) {
  $r = Read-Host "Instalar o launcher novo (com a sonda da re) agora? (s/n)"
  if ($r -eq 's') { & $ADB install -r $APK 2>&1 | Out-Host }
}
& $ADB shell appops set com.rocha.jancarlauncher SYSTEM_ALERT_WINDOW allow 2>&1 | Out-Null

Passo "1. Sistema e propriedades"
Sh "01-getprop-tudo" "getprop"
Sh "01-getprop-re" "getprop | grep -i -E 'ccd|backcar|reverse|camera|fastreverse|avm|jancar|canversion'"
Sh "01-id-shell" "id"
Sh "01-mounts" "mount | grep -E 'jancar|system|vendor'"

Passo "2. Apps de camera / re instalados"
Sh "02-pacotes-camera" "pm list packages -f | grep -i -E 'camera|backcar|avm|avin|carcameras|fourcamera|baony'"
Sh "02-pacotes-jancar" "pm list packages -f | grep -i -E 'jancar|autochips|can\.'"
$pkgs = @('com.autochips.camera','com.autochips.backcarapp','com.jancar.backcar','com.jancar.avin',
          'com.jancar.carcameras','com.baony.avm360','com.jancar.fourcamera','com.jancar.settings')
$instalados = @()
foreach ($p in $pkgs) {
  $path = (& $ADB shell pm path $p 2>&1 | Out-String).Trim()
  if ($path -like 'package:*') {
    $instalados += $p
    Sh "02-dumpsys-$p" "dumpsys package $p"
  }
}
Write-Host "  instalados: $($instalados -join ', ')"

Passo "3. Arquivos de configuracao (linhas, volume, telas de ajuste)"
Sh "03-ls-jancar" "ls -la /jancar /jancar/config"
Sh "03-ls-etc-ini" "ls -la /etc/ivi-*.ini"
foreach ($f in @('/jancar/config/ivi-config.ini','/etc/ivi-config.ini','/jancar/config/ivi-customer.ini',
                 '/etc/ivi-customer.ini','/jancar/config/ivi-settings.ini','/etc/ivi-settings.ini')) {
  $nome = '03-' + ($f.TrimStart('/') -replace '/','_')
  Sh $nome "cat $f"
}
Sh "03-teste-gravar-jancar-config" "touch /jancar/config/.teste_re && ls -l /jancar/config/.teste_re && rm /jancar/config/.teste_re && echo GRAVAVEL || echo NAO_GRAVAVEL"
Sh "03-settings-re" "settings list system | grep -i -E 'ccd|backcar|reverse|track|guide'; settings list global | grep -i -E 'ccd|backcar|reverse'; settings list secure | grep -i -E 'ccd|backcar|reverse'"

Passo "4. Camera no Android (para saber se da para fazer tela propria)"
Sh "04-media-camera" "dumpsys media.camera"
Sh "04-dev-video" "ls -la /dev/video* /dev/*cam* /dev/*cvbs* /dev/*tvd* 2>/dev/null"
Sh "04-servicos-camera" "dumpsys activity services com.autochips.camera"

Passo "5. Copia dos APKs (para descompilar depois)"
foreach ($p in $instalados) {
  $path = (& $ADB shell pm path $p 2>&1 | Out-String).Trim() -replace '^package:',''
  $dir = $path.Substring(0, $path.LastIndexOf('/'))
  $dest = Join-Path $BASE "apks\$p"
  New-Item -ItemType Directory -Force $dest | Out-Null
  & $ADB pull $dir $dest 2>&1 | Out-Null
  Write-Host "  ok  $p  ($dir)"
}

Passo "6. Log desde que a central ligou"
& $ADB logcat -d -v threadtime 2>&1 | Out-File -Encoding utf8 (Join-Path $OUT '06-logcat-boot.txt')
Write-Host "  ok  06-logcat-boot"

Passo "7. Antes da re (estado normal)"
Sh "07-janelas-antes" "dumpsys window windows"
Sh "07-topo-antes" "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity|topResumedActivity'"
Tela "07-tela-antes"

Passo "8. Camera aberta na mao, sem re"
Sh "08-abrir-camera-manual" "am start -W -n com.autochips.camera/.ui.activity.MainActivity"
Start-Sleep -Seconds 4
Tela "08-tela-camera-manual"
Sh "08-topo-camera-manual" "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity|topResumedActivity'"
Espera "Anote: a imagem da camera apareceu com a mao (sem re)? Depois aperte HOME na central."

# Log continuo a partir daqui (re, volume, volante)
& $ADB logcat -c 2>&1 | Out-Null
$logFile = Join-Path $OUT '09-logcat-re.txt'
$logProc = Start-Process -FilePath $ADB -ArgumentList 'logcat','-v','threadtime' -RedirectStandardOutput $logFile -NoNewWindow -PassThru

Passo "8b. Sonda da re (launcher)"
& $ADB shell am start -n com.rocha.jancarlauncher/.ReverseProbeActivity --ez overlay true 2>&1 | Out-Null
Start-Sleep -Seconds 4
Tela "08b-tela-sonda"
Espera "Na sonda: anote a linha 'Volume na re (15)' (valor/min/max). Linhas coloridas de teste devem estar na tela."

Passo "9. Com a re engatada"
Espera "Coloque uma musica tocando (volume normal). Freio de mao puxado. ENGATE A RE."
Start-Sleep -Seconds 3
Sh "09-janelas-re" "dumpsys window windows"
Sh "09-topo-re" "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity|topResumedActivity'"
Sh "09-surfaceflinger-re" "dumpsys SurfaceFlinger --list"
Tela "09-tela-re"
Espera "Anote: a musica abaixou? Quanto? As linhas COLORIDAS de teste (e o texto 'TESTE DO LAUNCHER') aparecem por cima da imagem da camera? Fotografe com o celular. As linhas da propria central aparecem? Sao fixas?"
Espera "Gire o volante todo para a esquerda e depois para a direita. Anote se as linhas da central se mexem e se o texto 'volante:' da camada de teste muda."
Tela "09-tela-re-volante"
Espera "Se o carro tiver sensor de estacionamento: chegue a mao/um objeto perto do para-choque traseiro. Anote se aparece algo na tela e se o texto 'sensores:' muda."
Tela "09-tela-re-sensor"

Passo "10. Saindo da re"
Espera "DESENGATE A RE."
Start-Sleep -Seconds 3
Sh "10-topo-depois" "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity|topResumedActivity'"
Tela "10-tela-depois"

Passo "10b. Volume da musica na re em 100"
& $ADB shell am start -n com.rocha.jancarlauncher/.ReverseProbeActivity 2>&1 | Out-Null
Espera "Na sonda, toque 'Volume na re: testar 100'. Confira se 'valor=' virou 100."
Espera "Com a musica tocando, ENGATE A RE de novo. Anote se a musica ainda abaixa."
Tela "10b-tela-re-volume100"
Espera "DESENGATE A RE. Se quiser manter a musica sem abaixar, deixe em 100. Para voltar como era, toque 'voltar ao original' na sonda."
Sh "10b-sonda-depois" "logcat -d -s ReProbe"

Passo "11. Telas de ajuste da re e do audio"
Sh "11-abrir-ajuste-re" "am start -W -n com.jancar.settings/.ui.activity.BackCarSettingActivity"
Start-Sleep -Seconds 3
Tela "11-tela-ajuste-re"
Espera "Anote quais opcoes aparecem no ajuste da re (linhas, espelhar, etc.). NAO mude nada ainda. Depois abra Configuracoes > Audio na central."
Tela "11-tela-ajuste-audio"
Espera "Anote se existe algum controle tipo 'volume na re' / 'volume da midia na re'. Role a tela se precisar (tire outra foto com o celular)."

& $ADB shell am start -n com.rocha.jancarlauncher/.ReverseProbeActivity 2>&1 | Out-Null
Espera "Na sonda, toque 'Encerrar teste' (as linhas de teste somem)."
Stop-Process -Id $logProc.Id -ErrorAction SilentlyContinue

Passo "12. Resumo do log da re"
$filtro = 'CCD =|isOn:|backcar|BackCar|Ccd|ccd|FastReverse|fastreverse|ivi-config|CameraService|autochips.camera|setMasterVolumePercent|ReProbe|radar=|volante=|aviso nao lido'
Select-String -Path $logFile -Pattern $filtro | ForEach-Object { $_.Line } |
  Out-File -Encoding utf8 (Join-Path $OUT '12-resumo-re.txt')
Select-String -Path (Join-Path $OUT '06-logcat-boot.txt') -Pattern $filtro | ForEach-Object { $_.Line } |
  Out-File -Encoding utf8 (Join-Path $OUT '12-resumo-boot.txt')
Write-Host "  ok  12-resumo-re / 12-resumo-boot"

Write-Host ""
Write-Host "Pronto. Tudo em: $OUT" -ForegroundColor Green
Write-Host "APKs em: $(Join-Path $BASE 'apks')" -ForegroundColor Green
