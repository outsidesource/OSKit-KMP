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
import kotlinx.coroutines.runBlocking
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorized
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.darwin.NSObject

internal class LocationKmpCapability(
    private val flags: Array<LocationCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val internalStateFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val delegate = object : CLLocationManagerDelegateProtocol, NSObject() {

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            scope.launch { internalStateFlow.emit(Unit) }
        }
    }

    private val locationManager by lazy {
        // CLLocationManager needs to be initialized on the main thread
        runBlocking(Dispatchers.Main) {
            CLLocationManager().apply { this.delegate = this@LocationKmpCapability.delegate }
        }
    }

    private val isCapabilityRequiredForFlags = when (flags.size) {
        1 -> flags[0] != LocationCapabilityFlags.BluetoothAccess
        else -> true
    }

    override val hasPermissions: Boolean = isCapabilityRequiredForFlags
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenEnableSettingsScreen: Boolean = false

    override val status: CapabilityStatus
        get() = getCurrentStatus()

    override val statusFlow: Flow<CapabilityStatus> = flow {
        emit(getCurrentStatus())
        emitAll(internalStateFlow.map { getCurrentStatus() })
    }.distinctUntilChanged()

    private var hardwareSupportsCapability: Boolean = true
    private var hasRequestedPermissions: Boolean = false

    override fun init(context: KmpCapabilityContext) {}

    private fun getCurrentStatus(): CapabilityStatus {
        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()
        if (!isCapabilityRequiredForFlags) return CapabilityStatus.Ready

        val hasAuthorization = when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorized,
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> true
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

        if (!CLLocationManager.locationServicesEnabled()) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        if (flags.contains(LocationCapabilityFlags.BackgroundLocation)) {
            locationManager.requestAlwaysAuthorization()
        } else {
            locationManager.requestWhenInUseAuthorization()
        }
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
