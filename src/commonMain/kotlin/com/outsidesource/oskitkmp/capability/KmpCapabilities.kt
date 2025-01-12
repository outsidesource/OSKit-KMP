package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow

expect class CapabilityContext

internal interface ICapabilityContextScope {
    val context: CapabilityContext?
}

internal expect suspend fun internalOpenAppSettingsScreen(context: CapabilityContext?): Outcome<Unit, Any>
internal expect fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability
internal expect fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability

class KmpCapabilities(
    bluetoothFlags: Array<BluetoothCapabilityFlags> = emptyArray(),
    locationFlags: Array<LocationCapabilityFlags> = emptyArray(),
) {
    private var context: CapabilityContext? = null

    /**
     * Android:
     * iOS: Add the following to info.plist
     *      NSBluetoothAlwaysUsageDescription
     *      NSBluetoothPeripheralUsageDescription
     */
    val bluetooth: IKmpCapability = createPlatformBluetoothCapability(bluetoothFlags)

    /**
     * 	Android:
     * 	iOS: Add the following to info.plist
     * 	    NSLocationAlwaysAndWhenInUseUsageDescription
     *   	NSLocationAlwaysUsageDescription
     * 	    NSLocationUsageDescription
     * 	    NSLocationWhenInUseUsageDescription
     */
    val location: IKmpCapability = createPlatformLocationCapability(locationFlags)

    fun init(context: CapabilityContext) {
        this.context = context
        (bluetooth as? IInitializableKmpCapability)?.init(context)
        (location as? IInitializableKmpCapability)?.init(context)
    }
}

enum class KmpCapabilitiesError {
    Unknown,
    Uninitialized,
    UnsupportedOperation,
}

enum class BluetoothCapabilityFlags {
    Scan,
    Connect,
    Advertise,
}

enum class LocationCapabilityFlags {
    BluetoothAccess, // Only required for Android <= 30
    BackgroundLocation,
    CoarseLocation,
    FineLocation,
}

internal interface IInitializableKmpCapability : IKmpCapability {
    fun init(context: CapabilityContext)
}

interface IKmpCapability {
    val status: CapabilityStatus
    val statusFlow: Flow<CapabilityStatus>

    val hasPermissions: Boolean
    val hasEnablableService: Boolean
    val supportsRequestEnable: Boolean
    val supportsOpenAppSettingsScreen: Boolean
    val supportsOpenEnableSettingsScreen: Boolean

    suspend fun requestPermissions(): Outcome<CapabilityStatus, Any>
    suspend fun requestEnable(): Outcome<CapabilityStatus, Any>
    suspend fun openEnableSettingsScreen(): Outcome<Unit, Any>
    suspend fun openAppSettingsScreen(): Outcome<Unit, Any>
}

sealed class CapabilityStatus {
    data object Unknown : CapabilityStatus()
    data class Unsupported(val reason: UnsupportedReason = UnsupportedReason.InvalidHardware) : CapabilityStatus()
    data class NoPermission(val reason: NoPermissionReason = NoPermissionReason.NotRequested) : CapabilityStatus()
    data object NotEnabled : CapabilityStatus()
    data object Ready : CapabilityStatus()
}

enum class UnsupportedReason {
    UnsupportedPlatform,
    InvalidHardware,
}

enum class NoPermissionReason {
    NotRequested,
    DeniedPermanently,
    Restricted,
}
