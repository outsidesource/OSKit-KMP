package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Native
import kotlinx.coroutines.CompletableDeferred
import org.freedesktop.dbus.DBusMatchRule
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.Tuple
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.net.URI
import kotlin.io.path.name
import kotlin.io.path.toPath
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
                mutableMapOf<String, Variant<*>>().apply {
                    this["handle_token"] = Variant(handleToken)
                    addFileFilter(filter)
                    addStartDirectory(startingDir)
                },
            )
        }

        if (result.code != 0) return Outcome.Ok(null)
        val refs = getRefsFromResponse(result, isDirectory = false)?.firstOrNull()
        return Outcome.Ok(refs)
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        val result = withDbusInterface<FileChooser> { windowId, handleToken ->
            OpenFile(
                windowId,
                "",
                mutableMapOf<String, Variant<*>>().apply {
                    this["handle_token"] = Variant(handleToken)
                    this["multiple"] = Variant(true)
                    addFileFilter(filter)
                    addStartDirectory(startingDir)
                },
            )
        }

        if (result.code != 0) return Outcome.Ok(null)
        val refs = getRefsFromResponse(result, isDirectory = false)
        return Outcome.Ok(refs)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        val result = withDbusInterface<FileChooser> { windowId, handleToken ->
            OpenFile(
                windowId,
                "",
                mutableMapOf<String, Variant<*>>().apply {
                    this["handle_token"] = Variant(handleToken)
                    this["directory"] = Variant(true)
                    addStartDirectory(startingDir)
                },
            )
        }

        if (result.code != 0) return Outcome.Ok(null)
        val refs = getRefsFromResponse(result, isDirectory = true)?.firstOrNull()
        return Outcome.Ok(refs)
    }

    override suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        val result = withDbusInterface<FileChooser> { windowId, handleToken ->
            SaveFile(
                windowId,
                "",
                mutableMapOf<String, Variant<*>>().apply {
                    this["handle_token"] = Variant(handleToken)
                    this["current_name"] = Variant(fileName)
                    addStartDirectory(startingDir)
                },
            )
        }

        if (result.code != 0) return Outcome.Ok(null)
        val refs = getRefsFromResponse(result, isDirectory = false)?.firstOrNull()
        return Outcome.Ok(refs)
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

    private fun MutableMap<String, Variant<*>>.addFileFilter(filter: KmpFileFilter?) = apply {
        if (filter == null) return@apply
        this["filters"] = Variant(
            listOf(Pair("Supported Files", filter.map { Pair(0, "*.${it.extension}") })),
            "a(sa(us))",
        )
    }

    private fun MutableMap<String, Variant<*>>.addStartDirectory(startingDir: KmpFsRef?) = apply {
        if (startingDir == null) return@apply
        this["current_folder"] = Variant(startingDir.ref.toByteArray() + 0x00)
    }

    private fun getRefsFromResponse(result: Response, isDirectory: Boolean): List<KmpFsRef>? {
        val uris = (result.results["uris"]?.value as? List<String>) ?: return null
        return uris.map {
            val path = URI(it).toPath()
            KmpFsRef(
                ref = path.toString(),
                name = path.name,
                fsType = KmpFsType.External,
                isDirectory = isDirectory,
            )
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
    }

    private class Pair<T1, T2>(
        @field:Position(0) val first: T1,
        @field:Position(1) val second: T2,
    ) : Tuple()
}
