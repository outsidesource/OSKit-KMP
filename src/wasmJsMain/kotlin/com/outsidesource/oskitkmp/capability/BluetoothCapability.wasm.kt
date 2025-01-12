package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BluetoothKmpCapability(
    private val flags: Array<BluetoothCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {
    override fun init(context: CapabilityContext) {}

    override val status: CapabilityStatus = if (hardwareSupportsCapability()) {
        CapabilityStatus.Ready
    } else {
        CapabilityStatus.Unsupported
    }

    override val statusFlow: Flow<CapabilityStatus> = flowOf(status)

    override val hasPermissions: Boolean = false
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenEnableSettingsScreen: Boolean = false

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}

private fun hardwareSupportsCapability(): Boolean = js("""navigator.bluetooth !== undefined""")
