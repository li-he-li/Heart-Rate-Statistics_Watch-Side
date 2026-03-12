package com.heartrate.desktop.data

import com.heartrate.shared.data.communication.WebSocketClient
import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Desktop-side repository backed by WebSocket stream from phone relay.
 */
class DesktopWebSocketHeartRateRepository(
    private val webSocketClient: WebSocketClient
) : HeartRateRepository {
    private var listening = false

    override fun observeHeartRate(): Flow<HeartRateData> {
        return if (listening) webSocketClient.heartRateDataFlow else emptyFlow()
    }

    override suspend fun startListening() {
        listening = true
    }

    override suspend fun stopListening() {
        listening = false
    }

    override suspend fun getBatteryLevel(): Int? = null

    override fun isListening(): Boolean = listening
}
