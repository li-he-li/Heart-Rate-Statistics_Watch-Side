        } else {
            log("known devices fallback produced no MAC")
        }
        return mac
    }

    private suspend fun readHeartRateOnWindows(mac: String): WindowsReadResult {
        val escapedMac = mac.replace("'", "''")
        val script = ps(
            """
            $ErrorActionPreference='Stop'
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            [void][Windows.Devices.Bluetooth.BluetoothLEDevice,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.BluetoothUuidHelper,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceService,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristic,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattReadResult,Windows.Devices.Bluetooth,ContentType=WindowsRuntime]
            [void][Windows.Storage.Streams.IBuffer,Windows.Storage.Streams,ContentType=WindowsRuntime]

            function Convert-ToTask([object]$asyncOperation, [Type]$resultType) {
                $method=[System.WindowsRuntimeSystemExtensions].GetMethods() |
                    Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetParameters().Count -eq 1 } |
                    Select-Object -First 1
                $genericMethod=$method.MakeGenericMethod($resultType)
                $genericMethod.Invoke($null, @($asyncOperation))
            }

            function Await-Result([object]$asyncOperation, [Type]$resultType, [int]$timeoutMs) {
                $task=Convert-ToTask $asyncOperation $resultType
                if(-not $task.Wait($timeoutMs)){ return $null }
                $task.Result
            }

            try {
                $addressString='$escapedMac'
                $address=[UInt64]::Parse(($addressString -replace ':',''), [System.Globalization.NumberStyles]::HexNumber)
                $device=Await-Result ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromBluetoothAddressAsync($address)) ([Windows.Devices.Bluetooth.BluetoothLEDevice]) 7000
                if(-not $device){ Write-Output 'ERR|DEVICE'; exit 0 }

                $serviceUuid=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x180D)
                $characteristicUuid=[Windows.Devices.Bluetooth.BluetoothUuidHelper]::FromShortId(0x2A37)
                $statusType=[Windows.Devices.Bluetooth.GenericAttributeProfile.GattCommunicationStatus]

                $servicesResult=Await-Result ($device.GetGattServicesForUuidAsync($serviceUuid,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult]) 7000
                if((-not $servicesResult) -or ($servicesResult.Status -ne $statusType::Success) -or ($servicesResult.Services.Count -eq 0)){
                    Write-Output 'ERR|SERVICE'
                    $device.Dispose()
                    exit 0
                }

                $service=$servicesResult.Services[0]
                $characteristicsResult=$null
                $serviceMethods=$service.PSObject.Methods.Name
                if($serviceMethods -contains 'GetCharacteristicsForUuidAsync'){
                    $characteristicsResult=Await-Result ($service.GetCharacteristicsForUuidAsync($characteristicUuid,[Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 7000
                } else {
                    $characteristicsResult=Await-Result ($service.GetCharacteristicsAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattCharacteristicsResult]) 7000
                }
                if((-not $characteristicsResult) -or ($characteristicsResult.Status -ne $statusType::Success) -or ($characteristicsResult.Characteristics.Count -eq 0)){
                    Write-Output 'ERR|CHAR'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }

                $characteristic=$null
                foreach($c in $characteristicsResult.Characteristics){
                    if($c.Uuid -eq $characteristicUuid){
                        $characteristic=$c
                        break
                    }
                }
                if(-not $characteristic){
                    Write-Output 'ERR|CHAR_UUID'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }
                $readResult=Await-Result ($characteristic.ReadValueAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattReadResult]) 7000
                if((-not $readResult) -or ($readResult.Status -ne $statusType::Success) -or (-not $readResult.Value)){
                    Write-Output 'ERR|READ'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }

                $toArrayMethod=[System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions].GetMethod(
                    'ToArray',
                    [Type[]]@([Windows.Storage.Streams.IBuffer])
                )
                if(-not $toArrayMethod){
                    Write-Output 'ERR|BUFFER_METHOD'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }
                try {
                    $bytes=$toArrayMethod.Invoke($null, @($readResult.Value))
                } catch {
                    Write-Output 'ERR|BUFFER'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }
                if($bytes.Length -lt 2){
                    Write-Output 'ERR|PAYLOAD'
                    $service.Dispose()
                    $device.Dispose()
                    exit 0
                }

                $flags=[int]$bytes[0]
                if(($flags -band 0x01) -eq 0){
                    $bpm=[int]$bytes[1]
                } else {
                    if($bytes.Length -lt 3){
