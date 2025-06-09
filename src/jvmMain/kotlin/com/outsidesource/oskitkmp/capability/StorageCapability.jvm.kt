package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StorageKmpCapability(
    private val flags: Array<StorageCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {

    private var context: KmpCapabilityContext? = null

    override fun init(context: KmpCapabilityContext) {
        this.context = context
    }

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
