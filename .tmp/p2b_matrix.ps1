$ErrorActionPreference = 'Stop'
$adb = 'D:\MyApplication\.android\platform-tools\adb.exe'
$phone = 'WCWODI8T4HYDGYYT'
$wear = 'emulator-5554'
$out = 'D:\MyApplication\.tmp\p2b_matrix_report.md'

function WS([int]$t=6){
  $uri=[Uri]'ws://127.0.0.1:18080/heartrate'; $ws=[System.Net.WebSockets.ClientWebSocket]::new(); $cts=[System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds($t));
  try { $ws.ConnectAsync($uri,$cts.Token).GetAwaiter().GetResult(); return $true } catch { return $false } finally { try{$ws.Dispose()}catch{} }
}
function Fatal([string]$serial){ return ((& $adb -s $serial logcat -d | Select-String -Pattern 'FATAL EXCEPTION').Count) }
function Alive([string]$serial,[string]$pkg,[string]$svc){ $d=& $adb -s $serial shell dumpsys activity services $pkg; return ((($d|Select-String -Pattern $svc).Count -gt 0) -and (($d|Select-String -Pattern 'isForeground=true').Count -gt 0)) }

$r = New-Object System.Collections.Generic.List[object]
function Add([string]$c,[string]$s,[string]$d){ $r.Add([pscustomobject]@{Case=$c;Status=$s;Details=$d}) | Out-Null }

# setup
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 5

# B1.3 pause/resume
& $adb -s $phone logcat -c; & $adb -s $wear logcat -c
& $adb -s $phone shell input keyevent 3 | Out-Null
& $adb -s $wear shell input keyevent 3 | Out-Null
Start-Sleep 4
$p1=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $w1=Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 4
$p2=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $w2=Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$fp=Fatal $phone; $fw=Fatal $wear
if($p1 -and $w1 -and $p2 -and $w2 -and $fp -eq 0 -and $fw -eq 0){ Add 'B1.3-A pause/resume' 'PASS' 'services alive + no fatal' } else { Add 'B1.3-A pause/resume' 'FAIL' "p1=$p1 w1=$w1 p2=$p2 w2=$w2 fatalPhone=$fp fatalWear=$fw" }

# B1.3 process recreation
& $adb -s $phone logcat -c; & $adb -s $wear logcat -c
& $adb -s $phone shell am kill com.heartrate.phone | Out-Null
& $adb -s $wear shell am kill com.heartrate.wear | Out-Null
Start-Sleep 3
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 5
$p3=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $w3=Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$fp=Fatal $phone; $fw=Fatal $wear
if($p3 -and $w3 -and $fp -eq 0 -and $fw -eq 0){ Add 'B1.3-B process recreation' 'PASS' 'services recovered + no fatal' } else { Add 'B1.3-B process recreation' 'FAIL' "p3=$p3 w3=$w3 fatalPhone=$fp fatalWear=$fw" }

# B4 ws disconnect/reconnect
& $adb -s $phone logcat -c
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$b=WS 5
& $adb -s $phone forward --remove-all | Out-Null
$d=WS 5
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$a=WS 5
$fp=Fatal $phone
if($b -and (-not $d) -and $a -and $fp -eq 0){ Add 'B4-1 ws disconnect/reconnect' 'PASS' 'connect/fail/reconnect ok' } else { Add 'B4-1 ws disconnect/reconnect' 'FAIL' "before=$b during=$d after=$a fatalPhone=$fp" }

# B4 permission deny/regrant
& $adb -s $wear logcat -c
& $adb -s $wear shell pm revoke com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 4
$fw1=Fatal $wear
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 5
$w4=Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$fw2=Fatal $wear
if($fw1 -eq 0 -and $w4 -and $fw2 -eq 0){ Add 'B4-2 permission deny/regrant' 'PASS' 'no fatal and recovered after grant' } else { Add 'B4-2 permission deny/regrant' 'FAIL' "fatalDeny=$fw1 w4=$w4 fatalAfterGrant=$fw2" }

# B4 service interruption phone
& $adb -s $phone logcat -c
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
Start-Sleep 2
$s=WS 5
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 5
$rec=WS 5
$p4=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$fp=Fatal $phone
if(((-not $s)) -and $rec -and $p4 -and $fp -eq 0){ Add 'B4-3 service interruption (phone)' 'PASS' 'stopped then recovered' } else { Add 'B4-3 service interruption (phone)' 'FAIL' "stoppedWs=$s recoveredWs=$rec p4=$p4 fatalPhone=$fp" }

# B4 data layer disconnected behavior
& $adb -s $wear logcat -c
70..76 | % { & $adb -s $wear emu sensor set heart-rate $_ | Out-Null; Start-Sleep -Milliseconds 700 }
Start-Sleep 3
$wl=& $adb -s $wear logcat -d
$n0=($wl|Select-String -Pattern 'connectedNodes=0').Count
$rt=($wl|Select-String -Pattern 'send retry in').Count
$fw=Fatal $wear
if($n0 -gt 0 -and $rt -gt 0 -and $fw -eq 0){ Add 'B4-4 data layer disconnected' 'PASS' "connectedNodes0=$n0 retry=$rt" } else { Add 'B4-4 data layer disconnected' 'BLOCKED' "connectedNodes0=$n0 retry=$rt fatalWear=$fw (pairing dependent)" }

$lines=@('# P2-B Matrix Report','',"Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",'', '| Case | Status | Details |','|---|---|---|')
foreach($x in $r){ $lines += "| $($x.Case) | $($x.Status) | $($x.Details -replace '\|','/') |" }
Set-Content -Path $out -Value $lines -Encoding UTF8
$r | Format-Table -AutoSize | Out-String
Write-Output "REPORT_PATH=$out"
