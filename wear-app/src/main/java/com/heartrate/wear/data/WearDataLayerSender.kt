package com.heartrate.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.heartrate.shared.data.communication.TransmissionPaths
import com.heartrate.shared.data.model.HeartRateData
import kotlinx.serialization.json.Json

/**
 * Sends heart-rate payloads from watch to phone through Wear Data Layer.
 */
class WearDataLayerSender(
    context: Context
) {
    private val nodeClient = Wearable.getNodeClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun send(data: HeartRateData): Result<Unit> = runCatching {
        val connectedNodes = nodeClient.connectedNodes.awaitResult()
        check(connectedNodes.isNotEmpty()) { "No paired phone is connected to this watch" }

        val payload = json.encodeToString(HeartRateData.serializer(), data).encodeToByteArray()
        connectedNodes.forEach { node ->
            messageClient.sendMessage(
                node.id,
                TransmissionPaths.HEART_RATE_MESSAGE_PATH,
                payload
            ).awaitResult()
        }
    }
}
