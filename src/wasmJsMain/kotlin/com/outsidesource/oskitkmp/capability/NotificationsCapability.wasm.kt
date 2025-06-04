package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class NotificationsKmpCapability() : IInitializableKmpCapability, IKmpCapability {

    private val localStatusFlow = MutableStateFlow<CapabilityStatus>(CapabilityStatus.Unknown)

    override val status: Flow<CapabilityStatus> = localStatusFlow

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override fun init(context: KmpCapabilityContext) {
        scope.launch {
            val status = queryPermissions().unwrapOrReturn { return@launch }
            localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status)

            status.onchange = { localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status) }
        }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        val status = queryPermissions().unwrapOrReturn { return CapabilityStatus.Unknown }
        return mapJsPermissionStatusToCapabilityStatus(status)
    }

    private suspend fun queryPermissions(): Outcome<PermissionStatus, Any> = navigator.permissions
        .query(permissionQueryParams("geolocation"))
        .kmpAwaitOutcome()

    private fun mapJsPermissionStatusToCapabilityStatus(status: PermissionStatus): CapabilityStatus {
        return when (status.state) {
            "granted" -> CapabilityStatus.Ready
            "prompt" -> CapabilityStatus.NoPermission(reason = NoPermissionReason.NotRequested)
            else -> CapabilityStatus.NoPermission(reason = NoPermissionReason.DeniedPermanently)
        }
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> = suspendCoroutine { continuation ->
        navigator.geolocation.getCurrentPosition(
            success = {
                localStatusFlow.value = CapabilityStatus.Ready
                continuation.resume(Outcome.Ok(CapabilityStatus.Ready))
            },
            error = {
                if (it.code == 1) {
                    localStatusFlow.value = CapabilityStatus.NoPermission(NoPermissionReason.DeniedPermanently)
                    continuation.resume(Outcome.Ok(localStatusFlow.value))
                    return@getCurrentPosition
                }
                continuation.resume(Outcome.Error(KmpCapabilitiesError.Unknown))
            },
        )
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}

private external object navigator {
    object permissions {
        fun query(options: JsAny): Promise<PermissionStatus>
    }

    object geolocation {
        fun getCurrentPosition(success: (JsAny) -> Unit, error: (GeolocationPositionError) -> Unit)
    }
}

private external class PermissionStatus : JsAny {
    val name: String
    val state: String
    var onchange: () -> Unit
}

private external class GeolocationPositionError {
    val code: Int
    val message: String
}

private fun permissionQueryParams(name: String): JsAny = js("""({ name: name })""")

private fun hardwareSupportsCapability(): Boolean = js("""navigator.geolocation !== undefined""")
