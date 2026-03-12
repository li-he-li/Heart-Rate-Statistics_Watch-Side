package com.heartrate.phone.data

import com.heartrate.shared.data.model.HeartRateData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process bridge between Data Layer listener service and relay repository.
 */
object PhoneHeartRateRelayBus {
    private val _heartRateFlow = MutableSharedFlow<HeartRateData>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val heartRateFlow: SharedFlow<HeartRateData> = _heartRateFlow.asSharedFlow()

    fun publish(data: HeartRateData) {
        _heartRateFlow.tryEmit(data)
    }
}
