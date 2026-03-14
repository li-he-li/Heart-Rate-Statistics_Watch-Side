package com.heartrate.phone.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.heartrate.phone.data.persistence.HeartRateDao
import com.heartrate.phone.data.persistence.HeartRateEntity
import com.heartrate.phone.network.PhoneBleGattServer
import com.heartrate.phone.network.PhoneWebSocketRelayServer
import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Phone-side repository: consumes watch data and relays to desktop via WebSocket.
 */
class PhoneRelayHeartRateRepository(
    private val appContext: Context,
    private val relayServer: PhoneWebSocketRelayServer,
    private val bleGattServer: PhoneBleGattServer,
    private val heartRateDao: HeartRateDao
) : HeartRateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var relayJob: Job? = null
    private var flushJob: Job? = null
    private var listening = false

    override fun observeHeartRate(): Flow<HeartRateData> = PhoneHeartRateRelayBus.heartRateFlow

    override suspend fun startListening() {
        if (listening) return
        listening = true
        relayServer.start()
        bleGattServer.start().onFailure {
            Log.w(TAG, "BLE fallback server start failed", it)
        }
        Log.i(TAG, "startListening: relay server started")
        relayJob?.cancel()
        relayJob = scope.launch {
            PhoneHeartRateRelayBus.heartRateFlow.collect { data ->
                runCatching {
                    val rowId = heartRateDao.insert(
                        HeartRateEntity.fromDomain(
                            data = data,
                            synced = false
                        )
                    )
                    if (relayViaBestChannel(data)) {
                        heartRateDao.markSynced(listOf(rowId))
                    }
                    Log.d(TAG, "relayed+bqueued bpm=${data.heartRate} ts=${data.timestamp}")
                }.onFailure { error ->
                    Log.e(TAG, "relay handling failed bpm=${data.heartRate}", error)
                }
            }
        }

        flushJob?.cancel()
        flushJob = scope.launch {
            flushPendingLoop()
        }
    }

    override suspend fun stopListening() {
        listening = false
        relayJob?.cancel()
        relayJob = null
        flushJob?.cancel()
        flushJob = null
        relayServer.stop()
        bleGattServer.stop()
        Log.i(TAG, "stopListening: relay server stopped")
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

    private suspend fun flushPendingLoop() {
        while (scope.coroutineContext.isActive) {
            runCatching {
                val pending = heartRateDao.getPending(FLUSH_BATCH_SIZE)
                if (pending.isEmpty()) {
                    delay(FLUSH_IDLE_DELAY_MS)
                    return@runCatching
                }

                val syncedIds = mutableListOf<Long>()
                pending.forEach { entity ->
                    val sent = relayViaBestChannel(entity.toDomain())
                    if (sent) {
                        syncedIds += entity.id
                    } else {
                        return@forEach
                    }
                }

                if (syncedIds.isNotEmpty()) {
                    heartRateDao.markSynced(syncedIds)
                    Log.d(TAG, "flushed pending=${syncedIds.size}")
                    delay(FLUSH_CONTINUE_DELAY_MS)
                } else {
                    delay(FLUSH_RETRY_DELAY_MS)
                }
            }.onFailure { error ->
                Log.e(TAG, "flushPendingLoop failed", error)
                delay(FLUSH_RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun relayViaBestChannel(data: HeartRateData): Boolean {
        if (relayServer.isRunning && relayServer.hasClients) {
            val wsResult = runCatching { relayServer.broadcast(data) }
            if (wsResult.isSuccess) {
                return true
            }
            Log.w(TAG, "WebSocket relay failed, fallback to BLE", wsResult.exceptionOrNull())
        }

        if (!bleGattServer.isRunning) {
            bleGattServer.start().onFailure {
                Log.w(TAG, "BLE fallback start failed", it)
            }
        }
        return bleGattServer.sendHeartRate(data).isSuccess
    }

    companion object {
        private const val TAG = "P2A-PhoneRepo"
        private const val FLUSH_BATCH_SIZE = 100
        private const val FLUSH_IDLE_DELAY_MS = 3_000L
        private const val FLUSH_CONTINUE_DELAY_MS = 300L
        private const val FLUSH_RETRY_DELAY_MS = 1_500L
    }
}
