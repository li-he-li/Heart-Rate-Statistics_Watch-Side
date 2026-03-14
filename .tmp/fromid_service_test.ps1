$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Enumeration.DeviceInformation,Windows.Devices.Enumeration,ContentType=WindowsRuntime]
function Convert-ToTask([object]$a,[Type]$t){ $m=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1; $m.MakeGenericMethod($t).Invoke($null,@($a)) }
function Await-Result([object]$a,[Type]$t,[int]$ms=12000){ $task=Convert-ToTask $a $t; if(-not $task.Wait($ms)){ return $null }; $task.Result }
$su=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
$cu=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x2A37)
$sel=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService]::GetDeviceSelectorFromUuid($su)
$devices=Await-Result ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($sel)) ([Windows.Devices.Enumeration.DeviceInformationCollection]) 12000
if(-not $devices -or $devices.Count -eq 0){ 'NO_DEVICES'; exit 0 }
$first=$devices[0]
'ID='+$first.Id
$service=Await-Result ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService]::FromIdAsync($first.Id)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService]) 12000
if(-not $service){ 'NO_SERVICE'; exit 0 }
'TYPE='+$service.GetType().FullName
'METHODS='+($service.PSObject.Methods.Name -join ',')
try {
  $cr=Await-Result ($service.GetCharacteristicsForUuidAsync($cu,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 12000
  if($cr){ 'CR_STATUS='+$cr.Status.ToString(); 'CR_COUNT='+$cr.Characteristics.Count } else { 'NO_CR' }
} catch { 'EX='+$_.Exception.Message }
