$ErrorActionPreference = 'Stop'
$adb='D:\MyApplication\.android\platform-tools\adb.exe'; $phone='WCWODI8T4HYDGYYT'; $wear='emulator-5554'
$out='D:\MyApplication\.tmp\p2b_matrix_report_v3.md'
function WS([int]$t=6){$u=[Uri]'ws://127.0.0.1:18080/heartrate';$w=[System.Net.WebSockets.ClientWebSocket]::new();$c=[System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds($t));try{$w.ConnectAsync($u,$c.Token).GetAwaiter().GetResult();$true}catch{$false}finally{try{$w.Dispose()}catch{}}}
function Alive([string]$s,[string]$pkg,[string]$svc){$d=& $adb -s $s shell dumpsys activity services $pkg; ((($d|Select-String $svc).Count -gt 0) -and (($d|Select-String 'isForeground=true').Count -gt 0)) }
function WaitAlive([string]$s,[string]$pkg,[string]$svc,[int]$timeoutSec=45){$tries=[int]($timeoutSec/5);for($i=0;$i -lt $tries;$i++){if(Alive $s $pkg $svc){return $true}; Start-Sleep 5}; return $false}
function CrashHits([string]$s,[string]$pkg){((& $adb -s $s logcat -d | Select-String -Pattern ("Process: " + [regex]::Escape($pkg))).Count)}
$r=New-Object System.Collections.Generic.List[object]; function Add([string]$c,[string]$s,[string]$d){$r.Add([pscustomobject]@{Case=$c;Status=$s;Details=$d})|Out-Null}

# setup
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $phone logcat -c; & $adb -s $wear logcat -c
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8

# B1.3 pause/resume
$bP=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $bW=WaitAlive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService' 45
& $adb -s $phone shell input keyevent 3 | Out-Null
& $adb -s $wear shell input keyevent 3 | Out-Null
Start-Sleep 8
$hP=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $hW=WaitAlive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService' 45
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8
$aP=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $aW=WaitAlive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService' 45
$cP=CrashHits $phone 'com.heartrate.phone'; $cW=CrashHits $wear 'com.heartrate.wear'
if($bP -and $bW -and $hP -and $hW -and $aP -and $aW -and $cP -eq 0 -and $cW -eq 0){Add 'B1.3-A pause/resume' 'PASS' 'services stable through home/resume'} else {Add 'B1.3-A pause/resume' 'FAIL' "bP=$bP bW=$bW hP=$hP hW=$hW aP=$aP aW=$aW cP=$cP cW=$cW"}

# B1.3 process recreation
& $adb -s $phone logcat -c; & $adb -s $wear logcat -c
& $adb -s $phone shell am kill com.heartrate.phone | Out-Null
& $adb -s $wear shell am kill com.heartrate.wear | Out-Null
Start-Sleep 4
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8
$rP=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'; $rW=WaitAlive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService' 45
$cP=CrashHits $phone 'com.heartrate.phone'; $cW=CrashHits $wear 'com.heartrate.wear'
if($rP -and $rW -and $cP -eq 0 -and $cW -eq 0){Add 'B1.3-B process recreation' 'PASS' 'services recovered after process kill'} else {Add 'B1.3-B process recreation' 'FAIL' "rP=$rP rW=$rW cP=$cP cW=$cW"}

# B4 ws disconnect/reconnect
& $adb -s $phone logcat -c
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$w1=WS 5
& $adb -s $phone forward --remove-all | Out-Null
$w2=WS 5
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$w3=WS 5
$cP=CrashHits $phone 'com.heartrate.phone'
if($w1 -and (-not $w2) -and $w3 -and $cP -eq 0){Add 'B4-1 ws disconnect/reconnect' 'PASS' 'expected connectivity transition'} else {Add 'B4-1 ws disconnect/reconnect' 'FAIL' "w1=$w1 w2=$w2 w3=$w3 cP=$cP"}

# B4 permission deny/regrant
& $adb -s $wear logcat -c
& $adb -s $wear shell pm revoke com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8
$denyAlive=Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8
$grantAlive=WaitAlive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService' 45
$cW=CrashHits $wear 'com.heartrate.wear'
if(((-not $denyAlive)) -and $grantAlive -and $cW -eq 0){Add 'B4-2 permission deny/regrant' 'PASS' 'deny->off, regrant->recovered, no crash'} else {Add 'B4-2 permission deny/regrant' 'FAIL' "denyAlive=$denyAlive grantAlive=$grantAlive cW=$cW"}

# B4 service interruption phone
& $adb -s $phone logcat -c
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
Start-Sleep 3
$s1=WS 5
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep 8
$s2=WS 5
$pAlive=Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$cP=CrashHits $phone 'com.heartrate.phone'
if(((-not $s1)) -and $s2 -and $pAlive -and $cP -eq 0){Add 'B4-3 service interruption (phone)' 'PASS' 'service restart recovered ws'} else {Add 'B4-3 service interruption (phone)' 'FAIL' "s1=$s1 s2=$s2 pAlive=$pAlive cP=$cP"}

# B4 data layer disconnected
& $adb -s $wear logcat -c
70..76 | % { & $adb -s $wear emu sensor set heart-rate $_ | Out-Null; Start-Sleep -Milliseconds 700 }
Start-Sleep 5
$wl=& $adb -s $wear logcat -d
$n0=($wl|Select-String 'connectedNodes=0').Count
$rt=($wl|Select-String 'send retry in').Count
$cW=CrashHits $wear 'com.heartrate.wear'
if($n0 -gt 0 -and $rt -gt 0 -and $cW -eq 0){Add 'B4-4 data layer disconnected' 'PASS' "connectedNodes0=$n0 retry=$rt"} else {Add 'B4-4 data layer disconnected' 'BLOCKED' "connectedNodes0=$n0 retry=$rt cW=$cW"}

$lines=@('# P2-B Matrix Report v3','',"Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",'', '| Case | Status | Details |','|---|---|---|')
foreach($x in $r){$lines += "| $($x.Case) | $($x.Status) | $($x.Details -replace '\|','/') |"}
Set-Content -Path $out -Value $lines -Encoding UTF8
$r | Format-Table -AutoSize | Out-String
Write-Output "REPORT_PATH=$out"
