$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
[void][Windows.Devices.Enumeration.DeviceInformation, Windows.Devices.Enumeration, ContentType = WindowsRuntime]
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
$selector = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelector()
$devices = Await-Result ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($selector)) ([Windows.Devices.Enumeration.DeviceInformationCollection]) 12000
if(-not $devices){ 'NO_DEVICES'; exit 0 }
foreach($d in $devices){
  if($d.Name -ne 'HeartRate Monitor'){ continue }
  $id=$d.Id
  $candidate = ($id -split '-')[-1]
  $hex = $candidate -replace ':',''
  if($hex -match '^[0-9A-Fa-f]{12}$'){ ($hex.ToUpperInvariant() -replace '(.{2})(?=.)','$1:') | Write-Output }
}
