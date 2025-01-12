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

class LocationKmpCapability(
    private val flags: Array<LocationCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {
    override fun init(context: CapabilityContext) {}

    private val localStatusFlow = MutableStateFlow<CapabilityStatus>(
        if (hardwareSupportsCapability()) {
            CapabilityStatus.Unknown
        } else {
            CapabilityStatus.Unsupported
        }
    )

    override val status: CapabilityStatus
        get() = localStatusFlow.value
    override val statusFlow: Flow<CapabilityStatus> = localStatusFlow

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenEnableSettingsScreen: Boolean = false

    init {
        scope.launch {
            if (!hardwareSupportsCapability()) return@launch

            val status = navigator.permissions
                .query(permissionQueryParams("geolocation"))
                .kmpAwaitOutcome()
                .unwrapOrReturn { return@launch }

            localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status)
            status.onchange = { localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status) }
        }
    }

    private fun mapJsPermissionStatusToCapabilityStatus(status: PermissionStatus): CapabilityStatus {
        return when (status.state) {
            "granted" -> CapabilityStatus.Ready
            "prompt" -> CapabilityStatus.NoPermission(reason = NoPermissionReason.NotRequested)
            else -> CapabilityStatus.NoPermission(reason = NoPermissionReason.DeniedPermanently)
        }
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> = suspendCoroutine { continuation ->
        navigator.geolocation.getCurrentPosition(
            success = { continuation.resume(Outcome.Ok(CapabilityStatus.Ready)) },
            error = {
                if (it.code == 1) {
                    continuation.resume(Outcome.Ok(localStatusFlow.value))
                    return@getCurrentPosition
                }
                continuation.resume(Outcome.Error(KmpCapabilitiesError.Unknown))
            },
        )
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> =
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
