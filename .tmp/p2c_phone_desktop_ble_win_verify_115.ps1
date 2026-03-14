$ErrorActionPreference = "Stop"

$adb = "D:\MyApplication\.android\platform-tools\adb.exe"
$phoneSerial = "16097851"
$reportPath = "D:\MyApplication\.tmp\p2c_phone_desktop_ble_win_verify_report.md"
$injectAction = "com.heartrate.phone.action.DEBUG_INJECT_HEART_RATE"
$injectReceiver = "com.heartrate.phone/.service.PhoneDebugInjectReceiver"
$bpmCases = @(115)

function Invoke-Adb([string[]]$adbArgs) {
    & $adb @adbArgs
}

function Convert-ToTask([object]$asyncOperation, [Type]$resultType) {
    $method = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object {
            $_.Name -eq "AsTask" -and
            $_.IsGenericMethodDefinition -and
            $_.GetParameters().Count -eq 1
        } |
        Select-Object -First 1
    $genericMethod = $method.MakeGenericMethod($resultType)
    $genericMethod.Invoke($null, @($asyncOperation))
}

function Await-Result([object]$asyncOperation, [Type]$resultType, [int]$timeoutMs = 10000) {
    $task = Convert-ToTask $asyncOperation $resultType
    if (-not $task.Wait($timeoutMs)) {
        return $null
    }
    return $task.Result
}

function Get-HeartRateTargetMac {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    [void][Windows.Devices.Bluetooth.BluetoothLEDevice, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Enumeration.DeviceInformation, Windows.Devices.Enumeration, ContentType = WindowsRuntime]

    $selector = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelector()
    $devices = Await-Result (
        [Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($selector)
    ) ([Windows.Devices.Enumeration.DeviceInformationCollection]) 12000
    if (-not $devices) { return $null }

    foreach ($d in $devices) {
        if ($d.Name -ne "HeartRate Monitor") { continue }
        $id = $d.Id
        if (-not $id) { continue }
        $candidate = ($id -split "-")[-1]
        $hex = $candidate -replace ":", ""
        if ($hex -match "^[0-9a-fA-F]{12}$") {
            return ($hex.ToUpperInvariant() -replace "(.{2})(?=.)", '$1:')
        }
    }
    return $null
}

function Read-HeartRateBpm([string]$mac) {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    [void][Windows.Devices.Bluetooth.BluetoothLEDevice, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.BluetoothUuidHelper, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristic, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattReadResult, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
    [void][Windows.Storage.Streams.IBuffer, Windows.Storage.Streams, ContentType = WindowsRuntime]

    $toArrayMethod = [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions].GetMethod(
        "ToArray",
        [Type[]]@([Windows.Storage.Streams.IBuffer])
    )
    if (-not $toArrayMethod) { return $null }

    $address = [UInt64]::Parse(($mac -replace ":", ""), [System.Globalization.NumberStyles]::HexNumber)
    $device = Await-Result (
        [Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($address)
    ) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 12000
    if (-not $device) { return $null }

    $service = $null
    try {
        $serviceUuid = [Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
        $characteristicUuid = [Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x2A37)
        $statusType = [Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]

        $servicesResult = Await-Result (
            $device.GetGattServicesForUuidAsync(
                $serviceUuid,
                [Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached
            )
        ) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 12000
        if ((-not $servicesResult) -or
            ($servicesResult.Status -ne $statusType::Success) -or
            ($servicesResult.Services.Count -eq 0)) {
            return $null
        }

        $service = $servicesResult.Services[0]
        $characteristicsResult = $null
        $serviceMethods = $service.PSObject.Methods.Name
        if ($serviceMethods -contains "GetCharacteristicsForUuidAsync") {
            $characteristicsResult = Await-Result (
                $service.GetCharacteristicsForUuidAsync(
                    $characteristicUuid,
                    [Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached
                )
            ) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 12000
        } else {
            $characteristicsResult = Await-Result (
                $service.GetCharacteristicsAsync(
                    [Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached
                )
            ) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 12000
        }
        if ((-not $characteristicsResult) -or
            ($characteristicsResult.Status -ne $statusType::Success) -or
            ($characteristicsResult.Characteristics.Count -eq 0)) {
            return $null
        }

        $characteristic = $characteristicsResult.Characteristics |
            Where-Object { $_.Uuid -eq $characteristicUuid } |
            Select-Object -First 1
        if (-not $characteristic) {
            return $null
        }
        $readResult = Await-Result (
            $characteristic.ReadValueAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)
        ) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattReadResult]) 12000
        if ((-not $readResult) -or
            ($readResult.Status -ne $statusType::Success) -or
            (-not $readResult.Value)) {
            return $null
        }

        $bytes = $toArrayMethod.Invoke($null, @($readResult.Value))
        if ((-not $bytes) -or ($bytes.Length -lt 2)) { return $null }
        $flags = [int]$bytes[0]
        if (($flags -band 0x01) -eq 0) {
            return [int]$bytes[1]
        }
        if ($bytes.Length -lt 3) { return $null }
        return ((([int]$bytes[2]) -shl 8) -bor ([int]$bytes[1]))
    } finally {
        if ($service) { try { $service.Dispose() } catch {} }
        try { $device.Dispose() } catch {}
    }
}

New-Item -ItemType Directory -Force -Path "D:\MyApplication\.tmp" | Out-Null

Invoke-Adb @("-s", $phoneSerial, "logcat", "-c") | Out-Null
Invoke-Adb @("-s", $phoneSerial, "shell", "am", "force-stop", "com.heartrate.phone") | Out-Null
Invoke-Adb @(
    "-s", $phoneSerial, "shell", "monkey",
    "-p", "com.heartrate.phone",
    "-c", "android.intent.category.LAUNCHER",
    "1"
) | Out-Null
Start-Sleep -Seconds 4

$targetMac = Get-HeartRateTargetMac
if (-not $targetMac) {
    Set-Content -Path $reportPath -Encoding UTF8 -Value @(
        "# P2-C Phone/Desktop BLE Windows Verification",
        "",
        "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
        "Result: FAIL",
        "Reason: Could not resolve `HeartRate Monitor` target MAC from Windows BLE device list."
    )
    Write-Output "REPORT_PATH=$reportPath"
    exit 1
}

$results = New-Object System.Collections.Generic.List[object]
foreach ($expectedBpm in $bpmCases) {
    Invoke-Adb @(
        "-s", $phoneSerial, "shell", "am", "broadcast",
        "-n", $injectReceiver,
        "-a", $injectAction,
        "--ei", "bpm", "$expectedBpm",
        "--ei", "count", "1",
        "--el", "intervalMs", "200"
    ) | Out-Null
    Start-Sleep -Milliseconds 900

    $actual = $null
    for ($i = 0; $i -lt 4; $i++) {
        $actual = Read-HeartRateBpm $targetMac
        if ($actual -ne $null) { break }
        Start-Sleep -Milliseconds 500
    }

    $status = if ($actual -eq $expectedBpm) { "PASS" } else { "FAIL" }
    $results.Add([PSCustomObject]@{
        Expected = $expectedBpm
        Actual = $actual
        Status = $status
    }) | Out-Null
}

$logLines = Invoke-Adb @("-s", $phoneSerial, "logcat", "-d")
$injectHits = ($logLines | Select-String -Pattern "PhoneDebugInject: injected sample").Count
$relayHits = ($logLines | Select-String -Pattern "P2A-PhoneRelayBus: publish bpm=").Count
$allPass = (($results | Where-Object { $_.Status -eq "FAIL" }).Count -eq 0)
$overall = if ($allPass) { "PASS" } else { "FAIL" }

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# P2-C Phone/Desktop BLE Windows Verification") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')") | Out-Null
$lines.Add("Phone: $phoneSerial") | Out-Null
$lines.Add("Target MAC: $targetMac") | Out-Null
$lines.Add("Overall: $overall") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("| Expected BPM | Actual BPM | Status |") | Out-Null
$lines.Add("|---|---|---|") | Out-Null
foreach ($r in $results) {
    $actualText = if ($r.Actual -eq $null) { "null" } else { "$($r.Actual)" }
    $lines.Add("| $($r.Expected) | $actualText | $($r.Status) |") | Out-Null
}
$lines.Add("") | Out-Null
$lines.Add("Phone log checks:") | Out-Null
$lines.Add("- PhoneDebugInject hits: $injectHits") | Out-Null
$lines.Add("- Relay publish hits: $relayHits") | Out-Null

Set-Content -Path $reportPath -Value $lines -Encoding UTF8

$results | Format-Table -AutoSize | Out-String
Write-Output "REPORT_PATH=$reportPath"
