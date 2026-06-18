package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Base for JVM capabilities that are not yet implemented. Reports [UnsupportedReason.NotImplemented]
 * and rejects every operation. Concrete capabilities exist only to provide distinct types.
 */
internal abstract class NotImplementedKmpCapability : IInitializableKmpCapability, IKmpCapability {
    override fun init(context: KmpCapabilityContext) {}

    override val status: Flow<CapabilityStatus> = flow { emit(queryStatus()) }
    override val hasPermissions: Boolean = false
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun queryStatus(): CapabilityStatus =
        CapabilityStatus.Unsupported(UnsupportedReason.NotImplemented)

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}

internal class BluetoothKmpCapability(
    flags: Array<BluetoothCapabilityFlags>,
) : NotImplementedKmpCapability()

internal class LocationKmpCapability(
    flags: Array<LocationCapabilityFlags>,
) : NotImplementedKmpCapability()

internal class MicrophoneKmpCapability : NotImplementedKmpCapability()
