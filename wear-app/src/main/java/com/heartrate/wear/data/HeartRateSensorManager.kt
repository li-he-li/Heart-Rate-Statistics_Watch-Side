package com.heartrate.wear.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
            val registered = sensorManager?.registerListener(
                listener,
                targetSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            ) ?: false
            check(registered) { "Failed to register heart rate sensor listener" }
            listening = true
        }
    }

    fun stop() {
        if (listening) {
            sensorManager?.unregisterListener(listener)
            listening = false
        }
    }

    fun isListening(): Boolean = listening

    private fun checkBodySensorsPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
        check(granted) { "BODY_SENSORS permission is required before starting monitoring" }
    }
}
