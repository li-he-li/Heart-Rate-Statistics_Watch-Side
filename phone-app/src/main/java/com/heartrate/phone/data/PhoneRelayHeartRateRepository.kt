package com.heartrate.phone.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.heartrate.phone.network.PhoneWebSocketRelayServer
import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Phone-side repository: consumes watch data and relays to desktop via WebSocket.
 */
class PhoneRelayHeartRateRepository(
    private val appContext: Context,
    private val relayServer: PhoneWebSocketRelayServer
) : HeartRateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var relayJob: Job? = null
    private var listening = false

    override fun observeHeartRate(): Flow<HeartRateData> = PhoneHeartRateRelayBus.heartRateFlow

    override suspend fun startListening() {
        if (listening) return
        listening = true
        relayServer.start()
        relayJob?.cancel()
        relayJob = scope.launch {
            PhoneHeartRateRelayBus.heartRateFlow.collect { data ->
                relayServer.broadcast(data)
            }
        }
    }

    override suspend fun stopListening() {
        listening = false
        relayJob?.cancel()
        relayJob = null
        relayServer.stop()
    }

    override suspend fun getBatteryLevel(): Int? {
        val statusIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = statusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = statusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100) / scale
    }

    override fun isListening(): Boolean = listening
}
