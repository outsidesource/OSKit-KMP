package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * TODO: Implement this properly for WASM
 */
class BluetoothCapability(private val flags: Array<BluetoothCapabilityFlags>) : IInitializableCapability, ICapability {
    override fun init(context: CapabilityContext) {}

    override val status: CapabilityStatus = CapabilityStatus.Ready
    override val statusFlow: Flow<CapabilityStatus> = flowOf(CapabilityStatus.Ready)

    override val hasPermissions: Boolean = false
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenEnableSettingsScreen: Boolean = false

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> =
        Outcome.Ok(CapabilityStatus.Ready)

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(CapabilityServiceError.UnsupportedOperation)

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(CapabilityServiceError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(CapabilityServiceError.UnsupportedOperation)
}
