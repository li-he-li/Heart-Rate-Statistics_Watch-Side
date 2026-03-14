Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
[Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService].Assembly.GetTypes() |
  Where-Object { $_.FullName -like '*GattDeviceService*' -or $_.FullName -like '*IGattDeviceService*' } |
  Select-Object -ExpandProperty FullName
