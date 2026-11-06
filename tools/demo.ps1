# 시연 영상용: Virtual Console을 명령으로 조작해 "충전기 연결" 상황을 재현합니다.
# 먼저 에뮬레이터를 켜고 한 번만:  adb forward tcp:8088 tcp:8088
#
# 사용법 (battery-guard 폴더에서)
#   powershell -ExecutionPolicy Bypass -File .\demo.ps1 reset    # 잔량 54%, 충전기 분리, 충전 한도 100%
#   powershell -ExecutionPolicy Bypass -File .\demo.ps1 plug     # 충전기 연결 (충전구 열림 + 충전 시작)
#   powershell -ExecutionPolicy Bypass -File .\demo.ps1 charge   # 잔량을 몇 초마다 1%씩 올림 (Ctrl+C로 중지)
#   powershell -ExecutionPolicy Bypass -File .\demo.ps1 unplug   # 충전기 분리
#   (앱에서 "프로토타입" 배지를 눌러 시연 배속 ×60을 켜면, 4초에 1%가 실제 시간당 약 15%(완속)로 환산됩니다)
#   ※ Virtual Console 화면의 충전 한도 값은 앱이 바꾼 값을 따라 바뀌지 않아서, charge는 -limit 값(기본 80%)에서 멈춥니다.
param([Parameter(Mandatory = $true)][ValidateSet("reset", "plug", "charge", "unplug", "state")][string]$step,
      [int]$intervalSec = 4,
      [int]$limit = 80)

$base = "http://localhost:8088/vehicle"

function Set-Ev([string]$field, $value) {
    # Virtual Console 웹 페이지가 백그라운드면 잠깐씩 "emulator_inactive"가 되므로 최대 5초 동안 다시 시도
    for ($try = 0; $try -lt 20; $try++) {
        try {
            Invoke-WebRequest "${base}/ev-battery?field=$field&value=$value" -Method Put -UseBasicParsing | Out-Null
            return
        } catch {
            Invoke-WebRequest "http://localhost:8088/browser/session?client=demo-ps1" -Method Post -UseBasicParsing -ErrorAction SilentlyContinue | Out-Null
            Start-Sleep -Milliseconds 250
        }
    }
    Write-Warning "$field=$value 설정 실패 (Virtual Console 페이지를 브라우저에서 열어 두세요)"
}

function Get-Ev { (Invoke-RestMethod "${base}/state").ev_battery }

switch ($step) {
    "reset" {
        # 앞 트렁크(프렁크)가 열려 있으면 왼쪽 차량 그림에 표시되므로 닫아 둔다
        try { Invoke-WebRequest "${base}/body?frunk=false" -Method Put -UseBasicParsing | Out-Null } catch {}
        Set-Ev charge_switch false
        Set-Ev charge_port_open false
        Set-Ev percent_limit 100
        Set-Ev soc 54
        Write-Output "준비 완료: 잔량 54%, 충전기 분리, 충전 한도 100%"
    }
    "plug" {
        Set-Ev charge_port_open true
        Set-Ev charge_switch true
        Write-Output "충전기 연결됨"
    }
    "charge" {
        while ($true) {
            $ev = Get-Ev
            $soc = [int]$ev.state_of_charge
            if ($soc -ge $limit) { Set-Ev charge_switch false; Write-Output "한도 $limit% 도달 → 충전 멈춤"; break }
            Set-Ev soc ($soc + 1)
            Write-Output "잔량 $($soc + 1)% (한도 $limit%)"
            Start-Sleep $intervalSec
        }
    }
    "unplug" {
        Set-Ev charge_switch false
        Set-Ev charge_port_open false
        Write-Output "충전기 분리됨"
    }
    "state" {
        $ev = Get-Ev
        Write-Output "잔량 $($ev.state_of_charge)% / 한도 $($ev.charge_limit_pct)% / 충전 $($ev.charging_activated) / 충전구 $($ev.charging_door_opened)"
    }
}
