package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow

expect class KmpCapabilityContext

internal interface ICapabilityContextScope {
    val context: KmpCapabilityContext?
}

internal expect suspend fun internalOpenAppSettingsScreen(context: KmpCapabilityContext?): Outcome<Unit, Any>
internal expect fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability
internal expect fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability
internal expect fun createPlatformNotificationsCapability(): IKmpCapability

/**
 * [KmpCapabilities] allows querying and requesting of permissions and enablement of certain platform capabilities.
 * Some capabilities (i.e. Bluetooth and Location) require both permissions to be granted and for their underlying
 * services to be enabled. [KmpCapabilities] aims to streamline that workflow.
 *
 * @param bluetoothFlags Indicates specific bluetooth features that are required. Most of the time flags map directly to
 * platform permissions
 *
 * @param locationFlags Indicates specific location features that are required. Most of the time flags map directly to
 * platform permissions
 */
class KmpCapabilities(
    bluetoothFlags: Array<BluetoothCapabilityFlags> = emptyArray(),
    locationFlags: Array<LocationCapabilityFlags> = emptyArray(),
) {
    private var context: KmpCapabilityContext? = null

    /**
     * iOS: Add the following to info.plist
     *      NSBluetoothAlwaysUsageDescription
     *      NSBluetoothPeripheralUsageDescription
     */
    val bluetooth: IKmpCapability = createPlatformBluetoothCapability(bluetoothFlags)

    /**
     * 	iOS: Add the following to info.plist
     * 	    NSLocationAlwaysAndWhenInUseUsageDescription
     *   	NSLocationAlwaysUsageDescription
     * 	    NSLocationUsageDescription
     * 	    NSLocationWhenInUseUsageDescription
     */
    val location: IKmpCapability = createPlatformLocationCapability(locationFlags)

    val notifications: IKmpCapability = createPlatformNotificationsCapability()

    fun init(context: KmpCapabilityContext) {
        this.context = context
        (bluetooth as? IInitializableKmpCapability)?.init(context)
        (location as? IInitializableKmpCapability)?.init(context)
        (notifications as? IInitializableKmpCapability)?.init(context)
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
    // Only required for Android <= 30. Android <= 30 requires location permissions and enablement to use bluetooth
    BluetoothAccess,
    BackgroundLocation,
    CoarseLocation,
    FineLocation,
}

interface IInitializableKmpCapability : IKmpCapability {
    fun init(context: KmpCapabilityContext)
}

interface IKmpCapability {
    /**
     * [status] A flow that immediately emits the current status of the capability and any subsequent changes
     */
    val status: Flow<CapabilityStatus>

    /**
     * [hasPermissions] True if there are any associated required permissions for the passed in flags
     */
    val hasPermissions: Boolean

    /**
     * [hasEnablableService] True if there is a platform service that must be on in order to use the capability
     */
    val hasEnablableService: Boolean

    /**
     * [supportsRequestEnable] True if the platform can explicitly request enablement of the underlying service
     */
    val supportsRequestEnable: Boolean

    /**
     * [supportsOpenAppSettingsScreen] True if app settings can be opened on the current platform
     */
    val supportsOpenAppSettingsScreen: Boolean

    /**
     * [supportsOpenServiceSettingsScreen] True if there is a specific settings page for the capability that can be
     * opened on the current platform
     */
    val supportsOpenServiceSettingsScreen: Boolean

    /**
     * [queryStatus] Queries the current status
     */
    suspend fun queryStatus(): CapabilityStatus

    /**
     * [requestPermissions] Requests the required permissions to use the capability
     */
    suspend fun requestPermissions(): Outcome<CapabilityStatus, Any>

    /**
     * [requestEnable] Requests the enablement of the service if the platform supports it
     */
    suspend fun requestEnable(): Outcome<CapabilityStatus, Any>

    /**
     * [openAppSettingsScreen] Opens the application settings screen if the platform supports it
     */
    suspend fun openAppSettingsScreen(): Outcome<Unit, Any>

    /**
     * [openServiceSettingsScreen] Opens the capability settings screen if the platform supports it
     */
    suspend fun openServiceSettingsScreen(): Outcome<Unit, Any>
}

sealed class CapabilityStatus {
    data object Unknown : CapabilityStatus()
    data class Unsupported(
        val reason: UnsupportedReason = UnsupportedReason.UnsupportedPlatformOrHardware,
    ) : CapabilityStatus()
    data class NoPermission(val reason: NoPermissionReason = NoPermissionReason.NotRequested) : CapabilityStatus()
    data object NotEnabled : CapabilityStatus()
    data object Ready : CapabilityStatus()
}

enum class UnsupportedReason {
    /**
     * [NotImplemented] The capability has not been implemented for platform in OSKit
     */
    NotImplemented,

    /**
     * [UnsupportedPlatformOrHardware] The underlying hardware or platform does not support the capability
     */
    UnsupportedPlatformOrHardware,
}

enum class NoPermissionReason {
    NotRequested,
    DeniedPermanently,
    Restricted,
}
