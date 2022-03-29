package com.outsidesource.oskitkmp.devTool

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

internal actual val devToolScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

actual class OSDevTool {
    private val sendFlow = MutableSharedFlow<DevToolEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual var isInitialized = false

    actual companion object {
        internal actual val instance: OSDevTool by lazy { OSDevTool() }

        actual fun init() {
            instance.startServer()
        }
    }

    private fun startServer() = devToolScope.launch {
        isInitialized = true

        embeddedServer(CIO, port = 7890) {
            install(Routing)
            install(WebSockets)

            routing {
                webSocket {
                    sendFlow.collect {
                        outgoing.send(Frame.Text(devToolJson.encodeToString(it)))
                    }
                }
            }
        }.start(wait = true)
    }

    actual suspend fun sendEvent(event: DevToolEvent) {
        sendFlow.emit(event)
    }
}
