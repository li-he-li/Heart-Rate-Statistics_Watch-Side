$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$winmd='C:\Windows\System32\WinMetadata\Windows.winmd'
$code=@"
using System;
using Windows.Devices.Bluetooth;
public static class T {
  public static string Ping(){
    var t = BluetoothLEDevice.GetDeviceSelector();
    return string.IsNullOrEmpty(t) ? "EMPTY" : "OK";
  }
}
"@
Add-Type -TypeDefinition $code -Language CSharp -ReferencedAssemblies $winmd
[T]::Ping() | Write-Output
