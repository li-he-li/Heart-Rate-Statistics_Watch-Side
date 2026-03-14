Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService].GetMethods() |
  Where-Object { $_.Name -like 'GetCharacteristics*' } |
  ForEach-Object { $_.ToString() }
