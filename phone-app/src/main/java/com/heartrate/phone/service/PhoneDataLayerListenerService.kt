package com.heartrate.phone.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.heartrate.phone.data.PhoneHeartRateRelayBus
import com.heartrate.shared.data.communication.TransmissionPaths
import com.heartrate.shared.data.model.HeartRateData
import kotlinx.serialization.json.Json

/**
 * Receives Data Layer messages from watch and pushes them into relay bus.
 */
class PhoneDataLayerListenerService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path != TransmissionPaths.HEART_RATE_MESSAGE_PATH) {
            return
        }

        val payload = messageEvent.data?.decodeToString() ?: return
        runCatching {
            json.decodeFromString(HeartRateData.serializer(), payload)
        }.onSuccess { data ->
            PhoneHeartRateRelayBus.publish(data)
        }.onFailure { error ->
            Log.e(TAG, "Failed to parse watch message", error)
        }
    }

    companion object {
        private const val TAG = "PhoneDataLayerListener"
    }
}
