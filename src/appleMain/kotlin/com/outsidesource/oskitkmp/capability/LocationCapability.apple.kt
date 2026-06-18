package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
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
) : AppleKmpCapability() {

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
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun queryStatus(): CapabilityStatus {
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
        return Outcome.Ok(queryStatus())
    }
}
