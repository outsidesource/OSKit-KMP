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
import java.net.ConnectException

private val client = HttpClient(CIO) { install(WebSockets) }

actual class OSDevToolClient {
    actual fun connect(
        host: String,
        port: Int,
    ): Flow<DevToolClientEvent> = callbackFlow {
        devToolScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port) {
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
