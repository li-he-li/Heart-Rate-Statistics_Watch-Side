$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
function Convert-ToTask([object]$a,[Type]$t){ $m=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1; $m.MakeGenericMethod($t).Invoke($null,@($a)) }
function Await-Result([object]$a,[Type]$t,[int]$ms=12000){ $task=Convert-ToTask $a $t; if(-not $task.Wait($ms)){ return $null }; $task.Result }
$mac='4D:02:A8:5E:1C:EF'
$addr=[UInt64]::Parse(($mac -replace ':',''),[System.Globalization.NumberStyles]::HexNumber)
$device=Await-Result ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($addr)) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 12000
if(-not $device){ 'NO_DEVICE'; exit 0 }
$su=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
$cu=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x2A37)
$st=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]
$sr=Await-Result ($device.GetGattServicesForUuidAsync($su,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 12000
if((-not $sr)-or($sr.Status -ne $st::Success)-or($sr.Services.Count -eq 0)){ 'NO_SERVICE'; exit 0 }
$service=$sr.Services[0]
'TYPE='+$service.GetType().FullName
try {
  $cr=Await-Result ($service.GetCharacteristicsForUuidAsync($cu,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 12000
  if($cr){ 'CR_STATUS='+$cr.Status.ToString(); 'CR_COUNT='+$cr.Characteristics.Count } else { 'NO_CR' }
} catch {
  'EX='+$_.Exception.Message
}
