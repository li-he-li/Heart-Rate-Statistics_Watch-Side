package com.heartrate.wear.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Wear-side repository: reads real sensor values and forwards them to phone.
 */
class WearHeartRateRepository(
    private val appContext: Context,
    private val sensorManager: HeartRateSensorManager,
    private val dataLayerSender: WearDataLayerSender
) : HeartRateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val outgoingFlow = MutableSharedFlow<HeartRateData>(replay = 0, extraBufferCapacity = 32)
    private var streamJob: Job? = null
    private var listening = false

    override fun observeHeartRate(): Flow<HeartRateData> = outgoingFlow.asSharedFlow()

    override suspend fun startListening() {
        if (listening) return
        sensorManager.start().getOrThrow()
        listening = true

        streamJob?.cancel()
        streamJob = scope.launch {
            sensorManager.heartRateReadings.collect { bpm ->
                val payload = HeartRateData(
                    timestamp = System.currentTimeMillis(),
                    heartRate = bpm,
                    deviceId = Build.MODEL ?: "wear-device",
                    batteryLevel = readBatteryLevel(),
                    signalQuality = 95
                )
                outgoingFlow.emit(payload)
                dataLayerSender.send(payload)
            }
        }
    }

    override suspend fun stopListening() {
        listening = false
        streamJob?.cancel()
        streamJob = null
        sensorManager.stop()
    }

    override suspend fun getBatteryLevel(): Int? = readBatteryLevel()

    override fun isListening(): Boolean = listening

    private fun readBatteryLevel(): Int? {
        val statusIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = statusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = statusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100) / scale
    }
}
