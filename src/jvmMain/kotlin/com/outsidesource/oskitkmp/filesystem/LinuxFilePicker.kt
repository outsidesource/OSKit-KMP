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

@OptIn(ExperimentalUuidApi::class)
class LinuxFilePicker(private val context: () -> KmpFsContext?) : IKmpFsFilePicker {

    override suspend fun pickFile(startingDir: KmpFsRef?, filter: KmpFileFilter?): Outcome<KmpFsRef?, KmpFsError> {
        return DBusConnectionBuilder.forSessionBus().build().use { conn ->
            val handleToken = Uuid.random().toString().replace("-", "")
            val windowId = Native.getWindowID(context()?.window).toString()
            val dbusObj = conn.getRemoteObject(
                "org.freedesktop.portal.Desktop",
                "/org/freedesktop/portal/desktop",
                FileChooserInterface::class.java,
            )

            val deferred = CompletableDeferred<Unit>()
            val matchRule = DBusMatchRule("signal", "org.freedesktop.portal.Request", "Response")
            val handler = DBusSigHandler<DBusSignal> { signal ->
                if (signal?.path?.endsWith(handleToken) != true) return@DBusSigHandler
                val response = (signal.parameters[0] as UInt32).toInt()
                println(response)
                val results = signal.parameters[1] as Map<String, Variant<Any>>
                println(results)
                deferred.complete(Unit)
            }
            conn.addGenericSigHandler(matchRule, handler)
            dbusObj.OpenFile(
                windowId,
                "",
                mutableMapOf(
                    "handle_token" to Variant(handleToken),
                ),
            )
            deferred.await()
            conn.removeGenericSigHandler(matchRule, handler)

            Outcome.Ok(null)
        }
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

    @DBusInterfaceName(value = "org.freedesktop.portal.FileChooser")
    internal interface FileChooserInterface : DBusInterface {
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
