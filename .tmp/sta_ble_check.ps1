$ErrorActionPreference='Stop'
$host.Runspace.ApartmentState | Write-Output
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
function Convert-ToTask([object]$a,[Type]$t){
  $m=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
  $m.MakeGenericMethod($t).Invoke($null,@($a))
}
function Await-Result([object]$a,[Type]$t,[int]$ms=12000){
  $task=Convert-ToTask $a $t
  if(-not $task.Wait($ms)){ return $null }
  $task.Result
}
$addr=[UInt64]::Parse('4D02A85E1CEF',[System.Globalization.NumberStyles]::HexNumber)
$device=Await-Result ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($addr)) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 12000
if(-not $device){ 'NO_DEVICE'; exit 0 }
$su=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
$st=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]
$sr=Await-Result ($device.GetGattServicesForUuidAsync($su,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 12000
if((-not $sr)-or($sr.Status -ne $st::Success)-or($sr.Services.Count -eq 0)){ 'NO_SERVICE'; exit 0 }
$service=$sr.Services[0]
'TYPE='+$service.GetType().FullName
'METHODS='+($service.PSObject.Methods.Name -join ',')
try {
  $cu=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x2A37)
  $cr=Await-Result ($service.GetCharacteristicsForUuidAsync($cu,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 12000
  if($cr){
    'CHR_STATUS='+$cr.Status.ToString()
    'CHR_COUNT='+$cr.Characteristics.Count
  } else {
    'NO_CHAR_RESULT'
  }
} catch {
  'EX='+$_.Exception.Message
}
