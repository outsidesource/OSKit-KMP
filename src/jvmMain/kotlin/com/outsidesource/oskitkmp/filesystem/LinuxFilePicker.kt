package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Native
import kotlinx.coroutines.CompletableDeferred
import org.freedesktop.dbus.DBusMatchRule
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A DBus desktop portal implementation for file pickers
 * https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.FileChooser.html
 */
@OptIn(ExperimentalUuidApi::class)
class LinuxFilePicker(private val context: () -> KmpFsContext?) : IKmpFsFilePicker {

    override suspend fun pickFile(startingDir: KmpFsRef?, filter: KmpFileFilter?): Outcome<KmpFsRef?, KmpFsError> {
        val result = withDbusInterface<FileChooser> { windowId, handleToken ->
            OpenFile(
                windowId,
                "",
                mutableMapOf(
                    "handle_token" to Variant(handleToken),
                ),
            )
        }

        println(result.results["uris"])

        return Outcome.Ok(null)
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        TODO("Not yet implemented")
    }

    override suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        TODO("Not yet implemented")
    }

    private suspend inline fun <reified T : DBusInterface> withDbusInterface(
        interfaceCall: T.(windowId: String, handleToken: String) -> Unit,
    ): Response {
        return DBusConnectionBuilder.forSessionBus().build().use { conn ->
            val handleToken = Uuid.random().toString().replace("-", "")
            val windowId = Native.getWindowID(context()?.window).toString()
            val dbusObj = conn.getRemoteObject(
                "org.freedesktop.portal.Desktop",
                "/org/freedesktop/portal/desktop",
                T::class.java,
            )

            val deferred = CompletableDeferred<Response>()
            val matchRule = DBusMatchRule("signal", "org.freedesktop.portal.Request", "Response")
            val handler = DBusSigHandler<DBusSignal> { signal ->
                if (signal?.path?.endsWith(handleToken) != true) return@DBusSigHandler
                val response = Response(
                    code = (signal.parameters[0] as UInt32).toInt(),
                    results = signal.parameters[1] as Map<String, Variant<Any>>,
                )
                deferred.complete(response)
            }
            conn.addGenericSigHandler(matchRule, handler)

            dbusObj.interfaceCall(windowId, handleToken)
            val response = deferred.await()
            conn.removeGenericSigHandler(matchRule, handler)
            response
        }
    }

    private data class Response(
        val code: Int,
        val results: Map<String, Variant<Any>>,
    )

    @DBusInterfaceName(value = "org.freedesktop.portal.FileChooser")
    private interface FileChooser : DBusInterface {
        fun OpenFile(
            parentWindow: String,
            title: String,
            options: MutableMap<String, Variant<*>>,
        ): DBusPath

        fun SaveFile(
            parentWindow: String,
            title: String,
            options: MutableMap<String, Variant<*>>,
        ): DBusPath

        fun SaveFiles(
            parentWindow: String,
            title: String,
            options: MutableMap<String, Variant<*>>,
        ): DBusPath
    }
}
