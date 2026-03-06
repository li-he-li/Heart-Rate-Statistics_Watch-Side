package com.heartrate.shared.presentation.viewmodel

import com.heartrate.shared.data.communication.BleClient
import com.heartrate.shared.data.communication.DataLayerClient
import com.heartrate.shared.data.communication.WebSocketClient
import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.usecase.GetBatteryLevel
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared ViewModel for Heart Rate monitoring
 *
 * This class provides the business logic for heart rate monitoring UI.
 * On Android platforms, this should be wrapped in a proper ViewModel.
 * On Desktop, this can be used directly.
 *
 * Note: For Phase 1, communication clients are simplified.
 * Phase 2 will integrate full Data Layer API, WebSocket, and BLE functionality.
 *
 * @param observeHeartRate Use case for observing heart rate data
 * @param getBatteryLevel Use case for getting battery level
 */
class HeartRateViewModel(
    private val observeHeartRate: ObserveHeartRate,
    private val getBatteryLevel: GetBatteryLevel
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(HeartRateUiState())
    val uiState: StateFlow<HeartRateUiState> = _uiState.asStateFlow()

    /**
     * Start monitoring heart rate
     */
    fun startMonitoring() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isMonitoring = true)

                // Start heart rate monitoring
                observeHeartRate.start()

                // Collect heart rate data in a separate job
                launch {
                    observeHeartRate()
                        .catch { error: Throwable ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Monitoring error: ${error.message}"
                            )
                        }
                        .collect { data ->
                            _uiState.value = _uiState.value.copy(
                                currentHeartRate = data.heartRate,
                                deviceInfo = data.deviceId,
                                errorMessage = null
                            )
                        }
                }

                // Collect battery level in a separate job
                launch {
                    while (true) {  // Continuously poll for battery level
                        try {
                            val battery = getBatteryLevel()
                            _uiState.value = _uiState.value.copy(batteryLevel = battery)
                            kotlinx.coroutines.delay(5000)  // Poll every 5 seconds
                        } catch (e: Throwable) {
                            // Battery errors are not critical, continue
                            kotlinx.coroutines.delay(10000)  // Wait longer on error
                        }
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isMonitoring = false,
                    errorMessage = "Failed to start monitoring: ${e.message}"
                )
            }
        }
    }

    /**
     * Stop monitoring heart rate
     */
    fun stopMonitoring() {
        viewModelScope.launch {
            try {
                observeHeartRate.stop()

                _uiState.value = _uiState.value.copy(
                    isMonitoring = false,
                    currentHeartRate = 0,
                    connectionStatus = ConnectionStatus.DISCONNECTED
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to stop monitoring: ${e.message}"
                )
            }
        }
    }

    /**
     * Connect to WebSocket server (Phone → Desktop)
     * Phase 1: Mock implementation
     */
    fun connectWebSocket(serverUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTING
            )
            // Phase 1: Mock connection
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTED
            )
        }
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnectWebSocket() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
        }
    }

    /**
     * Start BLE advertising (Phone) or scanning (Desktop)
     * Phase 1: Mock implementation
     */
    fun startBLE(serviceName: String = "HeartRateMonitor") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTING
            )
            // Phase 1: Mock BLE start
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTED
            )
        }
    }

    /**
     * Stop BLE
     */
    fun stopBLE() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Cleanup resources
     */
    fun onCleared() {
        stopMonitoring()
        disconnectWebSocket()
        stopBLE()
    }
}
