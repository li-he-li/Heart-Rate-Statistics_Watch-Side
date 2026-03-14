Add-Type -AssemblyName System.Runtime.WindowsRuntime
[System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeMarshal].GetMethods('Public,Static') | Select-Object -ExpandProperty Name | Sort-Object -Unique
