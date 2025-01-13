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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.darwin.NSObject

internal class BluetoothKmpCapability(
    private val flags: Array<BluetoothCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val internalStateFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val delegate = object : CBCentralManagerDelegateProtocol, NSObject() {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            scope.launch { internalStateFlow.emit(Unit) }
        }
    }

    private val central by lazy {
        CBCentralManager(delegate, null, mapOf(CBCentralManagerOptionShowPowerAlertKey to 0))
    }

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenEnableSettingsScreen: Boolean = false

    private var hardwareSupportsCapability: Boolean = true
    private var hasRequestedPermissions: Boolean = false

    override val status: CapabilityStatus
        get() = getCurrentStatus()

    override val statusFlow: Flow<CapabilityStatus> = flow {
        emit(getCurrentStatus())
        emitAll(internalStateFlow.map { getCurrentStatus() })
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {}

    private fun getCurrentStatus(): CapabilityStatus {
        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()

        val hasAuthorization = when (CBManager.authorization) {
            CBManagerAuthorizationAllowedAlways -> true
            else -> false
        }

        if (!hasAuthorization) {
            val reason = if (CBManager.authorization == CBManagerAuthorizationRestricted) {
                NoPermissionReason.Restricted
            } else if (hasRequestedPermissions) {
                NoPermissionReason.DeniedPermanently
            } else {
                NoPermissionReason.NotRequested
            }
            return CapabilityStatus.NoPermission(reason)
        }

        if (central.state != CBCentralManagerStatePoweredOn) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        central.state
        internalStateFlow.firstOrNull()
        hasRequestedPermissions = true
        internalStateFlow.emit(Unit)
        return Outcome.Ok(getCurrentStatus())
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(null)
}
