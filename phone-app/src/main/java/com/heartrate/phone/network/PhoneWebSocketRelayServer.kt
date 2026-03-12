package com.heartrate.phone.network

import com.heartrate.shared.data.model.HeartRateData
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Collections

/**
 * Phone-side WebSocket relay server for desktop clients.
 */
class PhoneWebSocketRelayServer(
    private val port: Int
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())
    private var engine: EmbeddedServer<*, *>? = null

    @Synchronized
    fun start() {
        if (engine != null) return
        engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                webSocket("/heartrate") {
                    sessions += this
                    try {
                        for (incomingFrame in incoming) {
                            if (incomingFrame is Frame.Close) break
                        }
                    } finally {
                        sessions -= this
                    }
                }
            }
        }.start(wait = false)
    }

    suspend fun broadcast(data: HeartRateData) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(HeartRateData.serializer(), data)
        val staleSessions = mutableListOf<DefaultWebSocketServerSession>()

        sessions.toList().forEach { session ->
            runCatching {
                session.send(Frame.Text(payload))
            }.onFailure {
                staleSessions += session
            }
        }

        if (staleSessions.isNotEmpty()) {
            sessions.removeAll(staleSessions.toSet())
            staleSessions.forEach { session ->
                runCatching { session.close() }
            }
        }
    }

    @Synchronized
    fun stop() {
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 1500)
        engine = null
        sessions.clear()
    }
}
