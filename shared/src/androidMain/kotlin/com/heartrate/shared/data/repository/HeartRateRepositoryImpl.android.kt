package com.heartrate.shared.data.repository

import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Android-specific mock implementation of HeartRateRepository.
 * Returns simulated heart rate data for testing purposes.
 */
actual class HeartRateRepositoryImpl actual constructor() : HeartRateRepository {

    private var listening = false
    private val mockData = generateMockData()

    override fun observeHeartRate(): Flow<HeartRateData> = flow {
        while (listening) {
            mockData.forEach { data ->
                if (listening) {
                    emit(data.copy(timestamp = System.currentTimeMillis()))
                    kotlinx.coroutines.delay(1000) // Emit every second
                }
            }
        }
    }

    override suspend fun startListening() {
        listening = true
    }

    override suspend fun stopListening() {
        listening = false
    }

    override fun isListening(): Boolean = listening

    override suspend fun getBatteryLevel(): Int? {
        // Simulate battery level between 70-100%
        return (70..100).random()
    }

    private fun generateMockData(): List<HeartRateData> {
        return listOf(
            HeartRateData(0, 72, "android-mock", batteryLevel = 85, signalQuality = 95),
            HeartRateData(0, 74, "android-mock", batteryLevel = 84, signalQuality = 94),
            HeartRateData(0, 73, "android-mock", batteryLevel = 83, signalQuality = 96),
            HeartRateData(0, 75, "android-mock", batteryLevel = 82, signalQuality = 93),
            HeartRateData(0, 71, "android-mock", batteryLevel = 81, signalQuality = 95),
        )
    }
}
