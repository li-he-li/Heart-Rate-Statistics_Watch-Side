$ErrorActionPreference = 'Stop'
$adb = 'D:\MyApplication\.android\platform-tools\adb.exe'
$phone = 'WCWODI8T4HYDGYYT'
$wear = 'emulator-5554'
$out = 'D:\MyApplication\.tmp\p2b_stability_30m_report.md'

function WS([int]$t=6){
  $uri=[Uri]'ws://127.0.0.1:18080/heartrate'; $ws=[System.Net.WebSockets.ClientWebSocket]::new(); $cts=[System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds($t));
  try { $ws.ConnectAsync($uri,$cts.Token).GetAwaiter().GetResult(); return $true } catch { return $false } finally { try{$ws.Dispose()}catch{} }
}
function Alive([string]$serial,[string]$pkg,[string]$svc){ $d=& $adb -s $serial shell dumpsys activity services $pkg; return ((($d|Select-String -Pattern $svc).Count -gt 0) -and (($d|Select-String -Pattern 'isForeground=true').Count -gt 0)) }

& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $phone logcat -c
& $adb -s $wear logcat -c
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8

$runStart = Get-Date
$wsOk = 0
$wsFail = 0
for($i=1; $i -le 180; $i++){
  $bpm = Get-Random -Minimum 66 -Maximum 96
  & $adb -s $wear emu sensor set heart-rate $bpm | Out-Null
  if($i % 6 -eq 0){ if(WS 5){$wsOk++} else {$wsFail++} }
  Start-Sleep 10
}
$runEnd = Get-Date
$duration = [math]::Round((($runEnd - $runStart).TotalMinutes), 2)

$wearLog = & $adb -s $wear logcat -d
$phoneLog = & $adb -s $phone logcat -d
$wearSamples = ($wearLog | Select-String -Pattern 'P2A-WearRepo: sensor sample bpm=').Count
$wearRetries = ($wearLog | Select-String -Pattern 'send retry in').Count
$wearCrashes = ($wearLog | Select-String -Pattern 'Process: com.heartrate.wear').Count
$phoneCrashes = ($phoneLog | Select-String -Pattern 'Process: com.heartrate.phone').Count
$wearAlive = Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$phoneAlive = Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$status = 'PASS'
if($duration -lt 30 -or $wearCrashes -gt 0 -or $phoneCrashes -gt 0 -or (-not $wearAlive) -or (-not $phoneAlive)) { $status = 'FAIL' }

$lines=@(
  '# P2-B 30-minute Stability Report',
  '',
  "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
  "Duration(min): $duration",
  "Status: $status",
  '',
  '| Metric | Value |',
  '|---|---|',
  "| wsOk | $wsOk |",
  "| wsFail | $wsFail |",
  "| wearSamples | $wearSamples |",
  "| wearRetries | $wearRetries |",
  "| wearProcessCrashHits | $wearCrashes |",
  "| phoneProcessCrashHits | $phoneCrashes |",
  "| wearServiceForegroundAlive | $wearAlive |",
  "| phoneServiceForegroundAlive | $phoneAlive |"
)
Set-Content -Path $out -Value $lines -Encoding UTF8
$lines -join "`n"
Write-Output "REPORT_PATH=$out"
