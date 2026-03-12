package com.heartrate.shared.presentation.viewmodel

import com.heartrate.shared.data.communication.BleClient
import com.heartrate.shared.data.communication.WebSocketClient
import com.heartrate.shared.domain.usecase.GetBatteryLevel
import com.heartrate.shared.domain.usecase.ObserveHeartRate
import com.heartrate.shared.presentation.model.ConnectionStatus
import com.heartrate.shared.presentation.model.HeartRateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    private val getBatteryLevel: GetBatteryLevel,
    private val webSocketClient: WebSocketClient? = null,
    private val bleClient: BleClient? = null
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var heartRateCollectionJob: Job? = null
    private var batteryPollingJob: Job? = null
    private var webSocketReconnectJob: Job? = null
    @Volatile
    private var webSocketManualDisconnect: Boolean = false

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
                heartRateCollectionJob?.cancel()
                heartRateCollectionJob = viewModelScope.launch {
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
                batteryPollingJob?.cancel()
                batteryPollingJob = viewModelScope.launch {
                    while (isActive) {
                        try {
                            val battery = getBatteryLevel()
                            _uiState.value = _uiState.value.copy(batteryLevel = battery)
                            delay(5000)
                        } catch (e: Throwable) {
                            // Battery errors are not critical, continue
                            delay(10000)
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
                heartRateCollectionJob?.cancel()
                heartRateCollectionJob = null
                batteryPollingJob?.cancel()
                batteryPollingJob = null
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
        val client = webSocketClient
        if (client == null) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTED,
                errorMessage = null
            )
            return
        }

        webSocketManualDisconnect = false
        webSocketReconnectJob?.cancel()
        webSocketReconnectJob = viewModelScope.launch {
            runWebSocketReconnectLoop(client = client, serverUrl = serverUrl)
        }
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnectWebSocket() {
        webSocketManualDisconnect = true
        webSocketReconnectJob?.cancel()
        webSocketReconnectJob = null
        viewModelScope.launch {
            runCatching { webSocketClient?.disconnect() }
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                errorMessage = null
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

            val client = bleClient
            if (client == null) {
                _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.CONNECTED)
                return@launch
            }

            client.startAdvertising(serviceName)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = ConnectionStatus.ERROR,
                        errorMessage = "BLE start failed: ${error.message}"
                    )
                }
        }
    }

    /**
     * Stop BLE
     */
    fun stopBLE() {
        viewModelScope.launch {
            runCatching { bleClient?.stopAdvertising() }
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
     * Detach UI observers without shutting down repository/service-level monitoring.
     * Used by Android activities to avoid interrupting foreground services.
     */
    fun detachUi() {
        heartRateCollectionJob?.cancel()
        heartRateCollectionJob = null
        batteryPollingJob?.cancel()
        batteryPollingJob = null
        webSocketManualDisconnect = true
        webSocketReconnectJob?.cancel()
        webSocketReconnectJob = null
    }

    /**
     * Cleanup resources
     */
    fun onCleared() {
        detachUi()
        webSocketReconnectJob?.cancel()
        webSocketReconnectJob = null
        heartRateCollectionJob?.cancel()
        heartRateCollectionJob = null
        batteryPollingJob?.cancel()
        batteryPollingJob = null
        stopMonitoring()
        disconnectWebSocket()
        stopBLE()
    }

    private suspend fun runWebSocketReconnectLoop(
        client: WebSocketClient,
        serverUrl: String
    ) {
        var backoffMs = WS_RECONNECT_INITIAL_MS

        while (viewModelScope.coroutineContext.isActive && !webSocketManualDisconnect) {
            _uiState.value = _uiState.value.copy(
                connectionStatus = ConnectionStatus.CONNECTING,
                errorMessage = null
            )

            val connectResult = client.connect(serverUrl)
            if (connectResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    errorMessage = null
                )
                backoffMs = WS_RECONNECT_INITIAL_MS

                while (viewModelScope.coroutineContext.isActive && !webSocketManualDisconnect && client.isConnected) {
                    delay(1000)
                }

                if (webSocketManualDisconnect) break

                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    errorMessage = "Connection lost, retrying..."
                )
            } else {
                val reason = connectResult.exceptionOrNull()?.message ?: "unknown error"
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "WebSocket connect failed: $reason"
                )
            }

            if (webSocketManualDisconnect) break
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(WS_RECONNECT_MAX_MS)
        }
    }

    companion object {
        private const val WS_RECONNECT_INITIAL_MS = 2000L
        private const val WS_RECONNECT_MAX_MS = 30000L
    }
}
