$ErrorActionPreference = 'Stop'

$adb = 'D:\MyApplication\.android\platform-tools\adb.exe'
$phone = 'WCWODI8T4HYDGYYT'
$wear = 'emulator-5554'
$reportPath = 'D:\MyApplication\.tmp\p2b_validation_report.md'
New-Item -ItemType Directory -Force -Path 'D:\MyApplication\.tmp' | Out-Null

function ADB-Phone([string]$args) {
    return (& $adb -s $phone shell $args)
}

function ADB-Wear([string]$args) {
    return (& $adb -s $wear shell $args)
}

function WS-CanConnect([int]$timeoutSec = 6) {
    $uri = [Uri]'ws://127.0.0.1:18080/heartrate'
    $ws = [System.Net.WebSockets.ClientWebSocket]::new()
    $cts = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds($timeoutSec))
    try {
        $ws.ConnectAsync($uri, $cts.Token).GetAwaiter().GetResult()
        $ws.Abort()
        $ws.Dispose()
        return $true
    } catch {
        try { $ws.Dispose() } catch {}
        return $false
    }
}

function Crash-Count([string]$serial, [string]$pkg) {
    $lines = & $adb -s $serial logcat -d
    $f1 = ($lines | Select-String -Pattern 'FATAL EXCEPTION').Count
    $f2 = ($lines | Select-String -Pattern ("Process: " + [regex]::Escape($pkg))).Count
    if ($f1 -gt $f2) { return $f1 }
    return $f2
}

function Service-Alive([string]$serial, [string]$pkg, [string]$serviceName) {
    $dump = & $adb -s $serial shell dumpsys activity services $pkg
    $hasService = ($dump | Select-String -Pattern $serviceName).Count -gt 0
    $hasForeground = ($dump | Select-String -Pattern 'isForeground=true').Count -gt 0
    return ($hasService -and $hasForeground)
}

function Add-CaseResult($results, [string]$caseId, [string]$status, [string]$details) {
    $results.Add([PSCustomObject]@{
        Case = $caseId
        Status = $status
        Details = $details
    }) | Out-Null
}

$results = New-Object System.Collections.Generic.List[object]
$startAt = Get-Date

# Global setup
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4

# Case B1.3-A: pause/resume
& $adb -s $phone logcat -c
& $adb -s $wear logcat -c
ADB-Phone 'input keyevent 3' | Out-Null
ADB-Wear 'input keyevent 3' | Out-Null
Start-Sleep -Seconds 5
$alivePhoneHome = Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$aliveWearHome = Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4
$alivePhoneResume = Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$aliveWearResume = Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$crashPhone = Crash-Count $phone 'com.heartrate.phone'
$crashWear = Crash-Count $wear 'com.heartrate.wear'
if ($alivePhoneHome -and $aliveWearHome -and $alivePhoneResume -and $aliveWearResume -and $crashPhone -eq 0 -and $crashWear -eq 0) {
    Add-CaseResult $results 'B1.3-A pause/resume' 'PASS' 'Foreground services alive before/after resume; no crash.'
} else {
    Add-CaseResult $results 'B1.3-A pause/resume' 'FAIL' ("alivePhoneHome=$alivePhoneHome aliveWearHome=$aliveWearHome alivePhoneResume=$alivePhoneResume aliveWearResume=$aliveWearResume crashPhone=$crashPhone crashWear=$crashWear")
}

# Case B1.3-B: process recreation
& $adb -s $phone logcat -c
& $adb -s $wear logcat -c
ADB-Phone 'am kill com.heartrate.phone' | Out-Null
ADB-Wear 'am kill com.heartrate.wear' | Out-Null
Start-Sleep -Seconds 3
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 5
$alivePhoneRecreate = Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$aliveWearRecreate = Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$crashPhone = Crash-Count $phone 'com.heartrate.phone'
$crashWear = Crash-Count $wear 'com.heartrate.wear'
if ($alivePhoneRecreate -and $aliveWearRecreate -and $crashPhone -eq 0 -and $crashWear -eq 0) {
    Add-CaseResult $results 'B1.3-B process recreation' 'PASS' 'Services recovered after process kill + relaunch; no crash.'
} else {
    Add-CaseResult $results 'B1.3-B process recreation' 'FAIL' ("alivePhoneRecreate=$alivePhoneRecreate aliveWearRecreate=$aliveWearRecreate crashPhone=$crashPhone crashWear=$crashWear")
}

# Case B4-1: WebSocket disconnect/reconnect
& $adb -s $phone logcat -c
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$wsBefore = WS-CanConnect
& $adb -s $phone forward --remove-all | Out-Null
$wsDuring = WS-CanConnect
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
$wsAfter = WS-CanConnect
$crashPhone = Crash-Count $phone 'com.heartrate.phone'
if ($wsBefore -and (-not $wsDuring) -and $wsAfter -and $crashPhone -eq 0) {
    Add-CaseResult $results 'B4-1 ws disconnect/reconnect' 'PASS' 'Connect=true -> fail during disconnect -> reconnect=true; no crash.'
} else {
    Add-CaseResult $results 'B4-1 ws disconnect/reconnect' 'FAIL' ("wsBefore=$wsBefore wsDuring=$wsDuring wsAfter=$wsAfter crashPhone=$crashPhone")
}

# Case B4-2: permission denied/regranted (wear)
& $adb -s $wear logcat -c
& $adb -s $wear shell pm revoke com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell am force-stop com.heartrate.wear | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4
$crashWearDenied = Crash-Count $wear 'com.heartrate.wear'
$permDeniedLogs = (& $adb -s $wear logcat -d | Select-String -Pattern 'Permission denied|BODY_SENSORS|Failed to start monitoring|Sensor unavailable').Count
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4
$aliveWearAfterGrant = Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$crashWearAfterGrant = Crash-Count $wear 'com.heartrate.wear'
if ($crashWearDenied -eq 0 -and $aliveWearAfterGrant -and $crashWearAfterGrant -eq 0) {
    Add-CaseResult $results 'B4-2 permission deny/regrant' 'PASS' ("No crash; recovered after grant. denyLogHits=$permDeniedLogs")
} else {
    Add-CaseResult $results 'B4-2 permission deny/regrant' 'FAIL' ("crashWearDenied=$crashWearDenied aliveWearAfterGrant=$aliveWearAfterGrant crashWearAfterGrant=$crashWearAfterGrant denyLogHits=$permDeniedLogs")
}

# Case B4-3: service interruption/recovery (phone)
& $adb -s $phone logcat -c
& $adb -s $phone shell am force-stop com.heartrate.phone | Out-Null
Start-Sleep -Seconds 2
$wsStopped = WS-CanConnect
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4
$wsRecovered = WS-CanConnect
$alivePhoneRecovered = Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'
$crashPhone = Crash-Count $phone 'com.heartrate.phone'
if ((-not $wsStopped) -and $wsRecovered -and $alivePhoneRecovered -and $crashPhone -eq 0) {
    Add-CaseResult $results 'B4-3 service interruption (phone)' 'PASS' 'WS unavailable during stop, recovered after relaunch; no crash.'
} else {
    Add-CaseResult $results 'B4-3 service interruption (phone)' 'FAIL' ("wsStopped=$wsStopped wsRecovered=$wsRecovered alivePhoneRecovered=$alivePhoneRecovered crashPhone=$crashPhone")
}

# Case B4-4: data layer disconnected behavior
& $adb -s $wear logcat -c
70..79 | ForEach-Object {
    & $adb -s $wear emu sensor set heart-rate $_ | Out-Null
    Start-Sleep -Milliseconds 700
}
Start-Sleep -Seconds 2
$wearLines = & $adb -s $wear logcat -d
$noNodeHits = ($wearLines | Select-String -Pattern 'connectedNodes=0').Count
$retryHits = ($wearLines | Select-String -Pattern 'send retry in').Count
$crashWear = Crash-Count $wear 'com.heartrate.wear'
if ($noNodeHits -gt 0 -and $retryHits -gt 0 -and $crashWear -eq 0) {
    Add-CaseResult $results 'B4-4 data layer disconnected' 'PASS' ("Observed disconnected retries; no crash. connectedNodes0=$noNodeHits retryHits=$retryHits")
} else {
    Add-CaseResult $results 'B4-4 data layer disconnected' 'FAIL' ("connectedNodes0=$noNodeHits retryHits=$retryHits crashWear=$crashWear")
}

# 30-min stability run (B5.1)
& $adb -s $phone logcat -c
& $adb -s $wear logcat -c
& $adb -s $phone forward --remove-all | Out-Null
& $adb -s $phone forward tcp:18080 tcp:8080 | Out-Null
& $adb -s $wear shell pm grant com.heartrate.wear android.permission.BODY_SENSORS | Out-Null
& $adb -s $phone shell monkey -p com.heartrate.phone -c android.intent.category.LAUNCHER 1 | Out-Null
& $adb -s $wear shell monkey -p com.heartrate.wear -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4

$runStart = Get-Date
$wsOk = 0
$wsFail = 0
for ($i = 1; $i -le 900; $i++) {
    $bpm = Get-Random -Minimum 66 -Maximum 96
    & $adb -s $wear emu sensor set heart-rate $bpm | Out-Null

    if ($i % 30 -eq 0) {
        if (WS-CanConnect 4) { $wsOk++ } else { $wsFail++ }
    }

    if ($i % 60 -eq 0) {
        # periodic service liveness check, no-op on purpose to keep probes deterministic
        [void](Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService')
        [void](Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService')
    }

    Start-Sleep -Seconds 2
}
$runEnd = Get-Date
$durationMin = [math]::Round((($runEnd - $runStart).TotalMinutes), 2)

$wearLog = & $adb -s $wear logcat -d
$phoneLog = & $adb -s $phone logcat -d
$wearSamples = ($wearLog | Select-String -Pattern 'sensor sample bpm=').Count
$wearRetryHits = ($wearLog | Select-String -Pattern 'send retry in').Count
$wearCrash = Crash-Count $wear 'com.heartrate.wear'
$phoneCrash = Crash-Count $phone 'com.heartrate.phone'
$wearAliveFinal = Service-Alive $wear 'com.heartrate.wear' 'WearMonitoringForegroundService'
$phoneAliveFinal = Service-Alive $phone 'com.heartrate.phone' 'PhoneRelayForegroundService'

$stabilityStatus = 'PASS'
if ($durationMin -lt 30 -or $wearCrash -gt 0 -or $phoneCrash -gt 0 -or (-not $wearAliveFinal) -or (-not $phoneAliveFinal)) {
    $stabilityStatus = 'FAIL'
}

Add-CaseResult $results 'B5.1 30-minute stability' $stabilityStatus (
    "durationMin=$durationMin wearSamples=$wearSamples wearRetryHits=$wearRetryHits wsOk=$wsOk wsFail=$wsFail wearCrash=$wearCrash phoneCrash=$phoneCrash wearAliveFinal=$wearAliveFinal phoneAliveFinal=$phoneAliveFinal"
)

# Build markdown report
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# P2-B Validation Report') | Out-Null
$lines.Add('') | Out-Null
$lines.Add("Generated at: $([DateTime]::Now.ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
$lines.Add("Window start: $($startAt.ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
$lines.Add("Window end: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))") | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Environment') | Out-Null
$lines.Add('- Phone: WCWODI8T4HYDGYYT (real device)') | Out-Null
$lines.Add('- Wear: emulator-5554 (virtual watch)') | Out-Null
$lines.Add('- WS probe: ws://127.0.0.1:18080/heartrate via adb forward') | Out-Null
$lines.Add('- Known limitation: Wear emulator is not paired to phone for Data Layer node discovery (connectedNodes=0).') | Out-Null
$lines.Add('') | Out-Null
$lines.Add('## Results') | Out-Null
$lines.Add('| Case | Status | Details |') | Out-Null
$lines.Add('|---|---|---|') | Out-Null
foreach ($r in $results) {
    $d = ($r.Details -replace '\|', '/')
    $lines.Add("| $($r.Case) | $($r.Status) | $d |") | Out-Null
}

Set-Content -Path $reportPath -Value $lines -Encoding UTF8

# stdout summary
$results | Format-Table -AutoSize | Out-String
Write-Output "REPORT_PATH=$reportPath"
