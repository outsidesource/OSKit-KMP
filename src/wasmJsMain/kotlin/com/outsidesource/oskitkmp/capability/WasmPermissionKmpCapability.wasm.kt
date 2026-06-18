@file:OptIn(ExperimentalWasmJsInterop::class)

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

/**
 * Base for browser capabilities backed by the Permissions API. Handles querying the current permission
 * state, reacting to permission changes via [PermissionStatus.onchange], and exposing it as [status].
 * Subclasses implement [requestPermissions], since the prompt mechanism differs per capability
 * (e.g. getUserMedia vs getCurrentPosition).
 *
 * @param permissionName The Permissions API name to query (e.g. "microphone", "geolocation").
 * @param hardwareSupported Whether the underlying browser API exists.
 * @param isCapabilityRequired False if the requested flags don't actually require this permission, in
 * which case the capability reports [CapabilityStatus.Ready] without prompting.
 */
internal abstract class WasmPermissionKmpCapability(
    private val permissionName: String,
    private val hardwareSupported: Boolean,
    protected val isCapabilityRequired: Boolean = true,
) : IInitializableKmpCapability, IKmpCapability {

    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    protected val localStatusFlow = MutableStateFlow(
        when {
            !hardwareSupported -> CapabilityStatus.Unsupported()
            !isCapabilityRequired -> CapabilityStatus.Ready
            else -> CapabilityStatus.Unknown
        },
    )

    override val status: Flow<CapabilityStatus> = localStatusFlow

    // Each capability must declare its full support surface explicitly — no inherited defaults.
    abstract override val hasPermissions: Boolean
    abstract override val hasEnablableService: Boolean
    abstract override val supportsRequestEnable: Boolean
    abstract override val supportsOpenAppSettingsScreen: Boolean
    abstract override val supportsOpenServiceSettingsScreen: Boolean

    override fun init(context: KmpCapabilityContext) {
        scope.launch {
            if (!hardwareSupported) return@launch
            if (!isCapabilityRequired) return@launch

            val status = queryPermissions().unwrapOrReturn { return@launch }
            localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status)

            status.onchange = { localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status) }
        }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        if (!hardwareSupported) return CapabilityStatus.Unsupported()
        if (!isCapabilityRequired) return CapabilityStatus.Ready

        val status = queryPermissions().unwrapOrReturn { return CapabilityStatus.Unknown }
        return mapJsPermissionStatusToCapabilityStatus(status)
    }

    private suspend fun queryPermissions(): Outcome<PermissionStatus, Any> = navigator.permissions
        .query(permissionQueryParams(permissionName))
        .kmpAwaitOutcome()

    protected fun mapJsPermissionStatusToCapabilityStatus(status: PermissionStatus): CapabilityStatus {
        return when (status.state) {
            "granted" -> CapabilityStatus.Ready
            "prompt" -> CapabilityStatus.NoPermission(reason = NoPermissionReason.NotRequested)
            else -> CapabilityStatus.NoPermission(reason = NoPermissionReason.DeniedPermanently)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}

private fun permissionQueryParams(name: String): JsAny = js("""({ name: name })""")
