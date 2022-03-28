package com.outsidesource.oskitkmp.devTool

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

internal actual val devToolScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

// TODO: Only log if debug build (https://kotlinlang.org/docs/multiplatform-configure-compilations.html#create-a-custom-compilation)
// TODO: Gzip messages
// TODO: Introduce batched throttling

actual class OSDevTool {
    private var serverStarted = atomic(false)
    private val sendFlow = MutableSharedFlow<DevToolEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    actual companion object {
        private val instance: OSDevTool by lazy { OSDevTool() }
        var isEnabled: Boolean = false

        internal actual fun sendEvent(event: DevToolEvent) = instance.sendEvent(event)
    }

    init {
        if (isEnabled) startServer()
    }

    private fun startServer() = devToolScope.launch {
        serverStarted.value = true

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

    private fun sendEvent(event: DevToolEvent) {
        devToolScope.launch {
            sendFlow.emit(event)
        }
    }
}
