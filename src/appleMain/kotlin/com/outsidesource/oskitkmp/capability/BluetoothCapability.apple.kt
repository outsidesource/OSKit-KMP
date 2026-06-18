package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

internal class BluetoothKmpCapability(
    private val flags: Array<BluetoothCapabilityFlags>,
) : AppleKmpCapability() {

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
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun queryStatus(): CapabilityStatus {
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

        awaitCentralPoweredOn(central)
        if (central.state != CBCentralManagerStatePoweredOn) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        central.state
        internalStateFlow.firstOrNull()
        hasRequestedPermissions = true
        internalStateFlow.emit(Unit)
        return Outcome.Ok(queryStatus())
    }
}

private suspend fun awaitCentralPoweredOn(central: CBCentralManager): Boolean {
    return withTimeoutOrNull(2_000.milliseconds) {
        while (isActive) {
            if (central.state == CBManagerStatePoweredOn) return@withTimeoutOrNull true
            delay(16.milliseconds)
        }
        false
    } ?: return false
}
