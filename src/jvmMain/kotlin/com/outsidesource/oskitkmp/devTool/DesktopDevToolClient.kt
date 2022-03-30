package com.outsidesource.oskitkmp.devTool

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.net.ConnectException

private val client = HttpClient(CIO) { install(WebSockets) }

actual class OSDevToolClient {
    actual fun connect(
        scheme: String,
        host: String,
        port: Int,
        path: String,
    ): Flow<DevToolClientEvent> = callbackFlow {
        devToolScope.launch {
            try {
                client.webSocket({
                    url.protocol = if (scheme.contains("wss")) URLProtocol.WSS else URLProtocol.WS
                    url.host = host
                    url.port = port
                    url.path(path.trimStart('/').split("/"))
                }) {
                    while (isActive) {
                        when (val frame = incoming.receive()) {
                            is Frame.Text -> {
                                val event = DevToolClientEvent.deserialize(frame.readText())
                                send(event)
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is ClosedReceiveChannelException -> close(OSDevToolClientError.ServerClosed)
                    is ConnectException -> close(OSDevToolClientError.InvalidHost)
                    is SerializationException -> close(OSDevToolClientError.UnknownEvent)
                    else -> {
                        e.printStackTrace()
                        close(OSDevToolClientError.Unknown)
                    }
                }
            }
        }

        awaitClose()
    }
}
