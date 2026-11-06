# 시연 영상 자동 촬영 (소리 포함): 에뮬레이터에서 시연 순서를 자동으로 진행하면서 에뮬레이터 내장 녹화기로 녹화합니다 (약 70초).
# 준비: 에뮬레이터 켜기, 배터리 지킴이 앱 설치, adb forward tcp:8088 tcp:8088, 가능하면 브라우저에 localhost:8088 열어 두기
#
# 사용법 (battery-guard 폴더에서)
#   powershell -ExecutionPolicy Bypass -File .\record_demo.ps1            → ..\시연영상_소리포함.webm 저장
#   powershell -ExecutionPolicy Bypass -File .\record_demo.ps1 -NoRecord  → 녹화 없이 진행만
#
# 장면: 기본 충전 화면(한도 100%) → 충전기 연결 → 제안 + 음성 → 80% 적용 + 음성
#       → 기본 충전 화면(한도 80%) → 충전 진행 → 관리 점수 → 리포트 → 충전 화면
# 녹화는 PC 스피커가 아니라 에뮬레이터 안의 소리를 담으므로, 스피커에서 소리가 깨져도 파일은 깨끗함.
# 에뮬레이터 녹화기는 한글 경로에 저장하지 못해 임시 폴더에 저장한 뒤 복사함.
param([switch]$NoRecord)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = Join-Path (Split-Path $PSScriptRoot -Parent) "시연영상_소리포함.webm"
$tmpRec = Join-Path $env:TEMP "batteryguard_demo.webm"
$APP = "app.batteryguard/.MainActivity"
$VEHICLE = "ai.umos.vehiclecontrol/.VehicleControlActivity"
Set-Location $PSScriptRoot

function Wait($s) { Start-Sleep -Milliseconds ([int]($s * 1000)) }
function Demo($step) { powershell -ExecutionPolicy Bypass -File .\demo.ps1 $step | Out-Null }
function Top { ((& $adb shell dumpsys activity activities | Select-String 'topResumedActivity' | Select-Object -First 1).Line) }
function Front($component) {
    # 원하는 앱이 실제로 맨 앞에 올 때까지 최대 3번 띄운다
    for ($i = 0; $i -lt 3; $i++) {
        & $adb shell am start -n $component | Out-Null; Wait 1.5
        if ((Top) -like "*$($component.Split('/')[0])*") { return }
        & $adb shell input keyevent KEYCODE_HOME; Wait 1
    }
    Write-Warning "$component 를 앞으로 띄우지 못함"
}
function TapText($text) {
    # 화면에서 글자를 찾아 그 가운데를 누른다 (좌표를 외워 두지 않아도 됨)
    & $adb shell uiautomator dump /sdcard/ui.xml | Out-Null
    $xml = (& $adb shell cat /sdcard/ui.xml) -join ''
    $m = [regex]::Match($xml, "text=`"$([regex]::Escape($text))`"[^>]*?bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if (-not $m.Success) { Write-Warning "'$text' 를 화면에서 찾지 못함"; return }
    $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2; $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
    & $adb shell input tap $x $y
}

# Virtual Console을 활성 상태로 유지
$keep = Start-Job { while ($true) { try { Invoke-WebRequest "http://localhost:8088/browser/session?client=bg-keepalive" -Method Post -UseBasicParsing -TimeoutSec 2 | Out-Null } catch {}; Start-Sleep -Milliseconds 400 } }
Wait 3

# ---------- 준비 ----------
Demo reset                                       # 잔량 54%, 충전기 분리, 한도 100%, 프렁크 닫기
& $adb shell am force-stop app.batteryguard
Front $APP; Wait 3
TapText "프로토타입"; Wait 1                       # 시연 배속 ×60 켜기
Front $VEHICLE; Wait 1
TapText "충전"; Wait 2                            # 기본 차량 앱을 [충전] 메뉴로

if (-not $NoRecord) {
    Remove-Item $tmpRec -ErrorAction SilentlyContinue
    & $adb emu screenrecord start --time-limit 80 --bit-rate 8000000 $tmpRec | Out-Null
    Wait 1
}

# ---------- 장면 ----------
Wait 4                                            # 기본 충전 화면: 한도 100%
Front $APP; Wait 2                                # 배터리 지킴이: 충전 대기
Demo plug; Wait 10                                # 충전기 연결 → 제안 + 음성
TapText "80%로 맞추기"; Wait 5                     # 적용 + 음성
Front $VEHICLE; Wait 6                            # 기본 충전 화면: 한도 80%
Front $APP
$ch = Start-Job { Set-Location $using:PSScriptRoot; powershell -ExecutionPolicy Bypass -File .\demo.ps1 charge -intervalSec 3 }
Wait 18; Stop-Job $ch; Remove-Job $ch             # 충전 진행
TapText "관리 점수"; Wait 5
TapText "리포트"; Wait 5
TapText "충전"; Wait 3

if (-not $NoRecord) {
    & $adb emu screenrecord stop | Out-Null
    Wait 5
    Copy-Item $tmpRec $out -Force
    Write-Output "저장: $out"
}
Stop-Job $keep; Remove-Job $keep
