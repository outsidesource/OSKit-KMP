package com.outsidesource.oskitkmp.devTool

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.encodeToString
import java.net.BindException

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

    private fun startServer(port: Int = 7890, retries: Int = 10) {
        devToolScope.launch {
            isInitialized = true

            try {
                coroutineScope { // Adding the coroutineScope fixes an issue where AndroidExceptionPreHandler throws java.lang.NoClassDefFoundError: android/os/Build$VERSION for some reason
                    println("DevTool Server running on 127.0.0.1:$port")

                    embeddedServer(factory = CIO, host = "127.0.0.1", port = port) {
                        install(Routing)
                        install(WebSockets)

                        routing {
                            webSocket {
                                sendFlow.collect {
                                    send(devToolJson.encodeToString(it))
                                }
                            }
                        }
                    }.start(wait = true)
                }
            } catch (e: BindException) {
                if (retries == 0) {
                    println("Could not start DevTool Server")
                    return@launch
                }
                startServer((1024..49151).random(), retries - 1)
            }
        }
    }

    actual suspend fun sendEvent(event: DevToolEvent) {
        sendFlow.emit(event)
    }
}
