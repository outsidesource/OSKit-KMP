package com.outsidesource.oskitkmp.devTool

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import java.net.ConnectException

private val client = HttpClient(CIO) { install(WebSockets) }

actual class OSDevToolClient {
    actual fun connect(
        scheme: String,
        host: String,
        port: Int,
        path: String,
    ): Flow<DevToolClientEvent> = flow {
        try {
            client.webSocket({
                url.protocol = if (scheme.contains("wss")) URLProtocol.WSS else URLProtocol.WS
                url.host = host
                url.port = port
                url.path(path.trimStart('/').split("/"))
            }) {
                emit(DevToolClientEvent.Status.Connected)

                while (isActive) {
                    when (val frame = incoming.receive()) {
                        is Frame.Text -> {
                            val event = DevToolClientEvent.deserialize(frame.readText())
                            emit(event)
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException ->
                    emit(DevToolClientEvent.Status.Error(DevToolClientError.ServerClosed))
                is ConnectException -> emit(DevToolClientEvent.Status.Error(DevToolClientError.InvalidHost))
                is SerializationException -> emit(DevToolClientEvent.Status.Error(DevToolClientError.UnknownEvent))
                else -> {
                    e.printStackTrace()
                    emit(DevToolClientEvent.Status.Error(DevToolClientError.Unknown))
                }
            }
            return@flow
        }
    }
}
