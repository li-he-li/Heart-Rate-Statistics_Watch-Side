package com.heartrate.wear.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wraps Wear OS heart-rate sensor access.
 */
class HeartRateSensorManager(
    private val context: Context
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val heartRateSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val _heartRateReadings = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var listening = false
    private var currentSamplingHz: Int = DEFAULT_SAMPLING_HZ

    val heartRateReadings: Flow<Int> = _heartRateReadings.asSharedFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val heartRate = event?.values?.firstOrNull()?.toInt() ?: return
            if (heartRate > 0) {
                _heartRateReadings.tryEmit(heartRate)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start(): Result<Unit> = runCatching {
        checkBodySensorsPermission()
        val targetSensor = heartRateSensor ?: error("Heart rate sensor not available on this watch")
        if (!listening) {
            val registered = registerListener(targetSensor, samplingHz = currentSamplingHz)
            check(registered) { "Failed to register heart rate sensor listener" }
            listening = true
            Log.i(
                TAG,
                "sensor listener registered type=${targetSensor.type} name=${targetSensor.name} hz=$currentSamplingHz"
            )
        }
    }

    fun stop() {
        if (listening) {
            sensorManager?.unregisterListener(listener)
            listening = false
            Log.i(TAG, "sensor listener unregistered")
        }
    }

    fun isListening(): Boolean = listening

    fun updateSamplingRate(targetHz: Int): Result<Unit> = runCatching {
        val safeHz = targetHz.coerceIn(MIN_SAMPLING_HZ, MAX_SAMPLING_HZ)
        if (safeHz == currentSamplingHz) return@runCatching
        currentSamplingHz = safeHz
        if (!listening) return@runCatching

        val targetSensor = heartRateSensor ?: error("Heart rate sensor not available on this watch")
        sensorManager?.unregisterListener(listener)
        val registered = registerListener(targetSensor, samplingHz = currentSamplingHz)
        check(registered) { "Failed to update sampling rate to ${currentSamplingHz}Hz" }
        Log.i(TAG, "sampling rate updated hz=$currentSamplingHz")
    }

    fun currentSamplingRateHz(): Int = currentSamplingHz

    private fun checkBodySensorsPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "BODY_SENSORS granted=$granted")
        check(granted) { "BODY_SENSORS permission is required before starting monitoring" }
    }

    private fun registerListener(sensor: Sensor, samplingHz: Int): Boolean {
        val manager = sensorManager ?: return false
        val periodUs = (1_000_000f / samplingHz).toInt()
        val reportLatencyUs = MAX_REPORT_LATENCY_US
        val preciseRegistered = manager.registerListener(
            listener,
            sensor,
            periodUs,
            reportLatencyUs
        )
        if (preciseRegistered) return true

        // Some Wear emulators reject custom period/latency combos for heart-rate.
        return manager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    companion object {
        private const val TAG = "P2A-WearSensor"
        private const val MIN_SAMPLING_HZ = 1
        private const val MAX_SAMPLING_HZ = 5
        private const val DEFAULT_SAMPLING_HZ = 1
        private const val MAX_REPORT_LATENCY_US = 1_000_000
    }
}
