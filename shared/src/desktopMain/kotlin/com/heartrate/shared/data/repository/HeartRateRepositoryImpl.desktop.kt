package com.heartrate.shared.data.repository

import com.heartrate.shared.data.model.HeartRateData
import com.heartrate.shared.domain.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Desktop-specific mock implementation of HeartRateRepository.
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
        // Desktop doesn't have battery monitoring in this mock
        return null
    }

    private fun generateMockData(): List<HeartRateData> {
        return listOf(
            HeartRateData(0, 68, "desktop-mock", batteryLevel = null, signalQuality = 98),
            HeartRateData(0, 70, "desktop-mock", batteryLevel = null, signalQuality = 97),
            HeartRateData(0, 69, "desktop-mock", batteryLevel = null, signalQuality = 99),
            HeartRateData(0, 71, "desktop-mock", batteryLevel = null, signalQuality = 96),
            HeartRateData(0, 67, "desktop-mock", batteryLevel = null, signalQuality = 98),
        )
    }
}
