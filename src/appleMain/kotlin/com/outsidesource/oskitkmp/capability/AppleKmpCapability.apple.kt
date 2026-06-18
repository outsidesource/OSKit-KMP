package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Shared Apple implementation of [IKmpCapability]. Provides the [status] flow plumbing (driven by
 * [internalStateFlow], which subclasses emit to from their platform delegates / permission requests)
 * along with the app-settings screen and the unsupported-by-default operations. Subclasses implement
 * [queryStatus] and [requestPermissions] against the relevant platform framework.
 */
internal abstract class AppleKmpCapability : IInitializableKmpCapability, IKmpCapability {

    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val internalStateFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    protected var hardwareSupportsCapability: Boolean = true
    protected var hasRequestedPermissions: Boolean = false

    // Each capability must declare its full support surface explicitly — no inherited defaults.
    abstract override val hasPermissions: Boolean
    abstract override val hasEnablableService: Boolean
    abstract override val supportsRequestEnable: Boolean
    abstract override val supportsOpenAppSettingsScreen: Boolean
    abstract override val supportsOpenServiceSettingsScreen: Boolean

    override val status: Flow<CapabilityStatus> = flow {
        emit(queryStatus())
        emitAll(internalStateFlow.map { queryStatus() })
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {}

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(null)
}
