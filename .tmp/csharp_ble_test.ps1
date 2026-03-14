$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$code=@"
using System;
using System.Threading.Tasks;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Devices.Bluetooth;
public static class T {
  public static string Ping(){
    var t = BluetoothLEDevice.GetDeviceSelector();
    return string.IsNullOrEmpty(t) ? "EMPTY" : "OK";
  }
}
"@
Add-Type -TypeDefinition $code -Language CSharp
[T]::Ping() | Write-Output
