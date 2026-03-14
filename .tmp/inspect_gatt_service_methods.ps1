$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]

function Convert-ToTask([object]$asyncOperation,[Type]$resultType){
  $method=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
  $generic=$method.MakeGenericMethod($resultType)
  $generic.Invoke($null,@($asyncOperation))
}
function Await-Result([object]$asyncOperation,[Type]$resultType,[int]$timeoutMs=10000){
  $task=Convert-ToTask $asyncOperation $resultType
  if(-not $task.Wait($timeoutMs)){ return $null }
  $task.Result
}
$mac='4D:02:A8:5E:1C:EF'
$addr=[UInt64]::Parse(($mac -replace ':',''),[System.Globalization.NumberStyles]::HexNumber)
$device=Await-Result ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($addr)) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 12000
if(-not $device){ 'NO_DEVICE'; exit 0 }
$serviceUuid=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
$statusType=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]
$servicesResult=Await-Result ($device.GetGattServicesForUuidAsync($serviceUuid,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 12000
if((-not $servicesResult) -or ($servicesResult.Status -ne $statusType::Success) -or ($servicesResult.Services.Count -eq 0)){ 'NO_SERVICE'; $device.Dispose(); exit 0 }
$service=$servicesResult.Services[0]
'SERVICE_TYPE='+$service.GetType().FullName
'METHODS_BEGIN'
$service.PSObject.Methods.Name | Sort-Object -Unique
'METHODS_END'
$service.Dispose(); $device.Dispose()
