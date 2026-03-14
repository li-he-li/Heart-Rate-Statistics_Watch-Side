$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
function Convert-ToTask([object]$a,[Type]$t){ $m=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1; $m.MakeGenericMethod($t).Invoke($null,@($a)) }
function Await-Result([object]$a,[Type]$t,[int]$ms=12000){ $task=Convert-ToTask $a $t; if(-not $task.Wait($ms)){ return $null }; $task.Result }
$mac='4D:02:A8:5E:1C:EF'; $addr=[UInt64]::Parse(($mac -replace ':',''),[System.Globalization.NumberStyles]::HexNumber)
$device=Await-Result ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($addr)) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 12000
$su=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
$st=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]
$sr=Await-Result ($device.GetGattServicesForUuidAsync($su,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 12000
$service=$sr.Services[0]
try {
  $all=$service.GetAllCharacteristics([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)
  if($all){ 'ALL_COUNT='+$all.Count } else { 'NO_ALL' }
} catch { 'EX='+$_.Exception.Message }
