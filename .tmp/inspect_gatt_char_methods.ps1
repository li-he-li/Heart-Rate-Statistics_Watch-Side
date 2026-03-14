Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristic,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristic].GetMethods() | Select-Object -ExpandProperty Name | Sort-Object -Unique
