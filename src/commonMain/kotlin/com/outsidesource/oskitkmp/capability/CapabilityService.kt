package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow

expect class CapabilityContext

internal interface ICapabilityContextScope {
    val context: CapabilityContext?
}

internal expect suspend fun internalOpenAppSettingsScreen(context: CapabilityContext?): Outcome<Unit, Any>
internal expect fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): ICapability
internal expect fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): ICapability
// internal expect fun createPlatformStorageCapability(flags: Array<StorageCapabilityFlags>): ICapability
// internal expect fun createPlatformCameraCapability(): ICapability
// internal expect fun createPlatformNetworkCapability(): ICapability

/**
 * TODO: Document each flag and what it corresponds to
 * TODO: Document the respective ios plist and Android permissions to include
 * TODO: Desktop support - Should everything be a noop?
 * TODO: Clear context listeners on activity destroy
 */
class CapabilityService(
    bluetoothFlags: Array<BluetoothCapabilityFlags> = emptyArray(),
    locationFlags: Array<LocationCapabilityFlags> = emptyArray(),
//    storageCapabilityFlags: Array<StorageCapabilityFlags> = emptyArray(),
) {
    private var context: CapabilityContext? = null

    /**
     * Android:
     * iOS: Add the following to info.plist
     *      NSBluetoothAlwaysUsageDescription
     *      NSBluetoothPeripheralUsageDescription
     */
    val bluetooth: ICapability = createPlatformBluetoothCapability(bluetoothFlags)

    /**
     * 	Android:
     * 	iOS: Add the following to info.plist
     * 	    NSLocationAlwaysAndWhenInUseUsageDescription
     *   	NSLocationAlwaysUsageDescription
     * 	    NSLocationUsageDescription
     * 	    NSLocationWhenInUseUsageDescription
     */
    val location: ICapability = createPlatformLocationCapability(locationFlags)
//    val storage: ICapability = createPlatformStorageCapability(storageCapabilityFlags)
//    val camera: ICapability = createPlatformCameraCapability()
//    val network: ICapability = createPlatformNetworkCapability()
//    val wifi: ICapability = createPlatformWifiCapability()

    fun init(context: CapabilityContext) {
        this.context = context
        (bluetooth as? IInitializableCapability)?.init(context)
        (location as? IInitializableCapability)?.init(context)
//        (storage as? IInitializableCapability)?.init(context)
//        (camera as? IInitializableCapability)?.init(context)
//        (network as? IInitializableCapability)?.init(context)
    }

    suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(context)
}

// Potential Permissions

// NFC

// INTERNET
// ACCESS_NETWORK_STATE
// ACCESS_WIFI_STATE
// NEARBY_WIFI_DEVICES - runtime

// POST_NOTIFICATIONS - runtime

// RECORD_AUDIO - runtime

// USE_BIOMETRIC

// VIBRATE

enum class CapabilityServiceError {
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

enum class StorageCapabilityFlags {
    ReadExternal,
    WriteExternal,
    ReadMedia,
    WriteMedia,

// ACCESS_MEDIA_LOCATION - runtime
// READ_EXTERNAL_STORAGE - runtime - <33
// READ_MEDIA_AUDIO - runtime >=33
// READ_MEDIA_IMAGES - runtime >=33
// READ_MEDIA_VIDEO - runtime >=33
// READ_MEDIA_VISUAL_USER_SELECTED - runtime
// WRITE_EXTERNAL_STORAGE - runtime (< Android R no effect) (>=19 not needed)
}

internal interface IInitializableCapability : ICapability {
    fun init(context: CapabilityContext)
}

// Note, iOS BLE central takes a second or two to determine if BLE is powered on or not
interface ICapability {
    val status: CapabilityStatus
    val statusFlow: Flow<CapabilityStatus>

    val hasPermissions: Boolean
    val hasEnablableService: Boolean
    val supportsRequestEnable: Boolean
    val supportsOpenEnableSettingsScreen: Boolean

    suspend fun requestPermissions(): Outcome<CapabilityStatus, Any>
    suspend fun requestEnable(): Outcome<CapabilityStatus, Any>
    suspend fun openEnableSettingsScreen(): Outcome<Unit, Any>
    suspend fun openAppSettingsScreen(): Outcome<Unit, Any>
}

sealed class CapabilityStatus {
    data object Unknown : CapabilityStatus()
    data object Unsupported : CapabilityStatus()
    data class NoPermission(val reason: NoPermissionReason = NoPermissionReason.NotRequested) : CapabilityStatus()
    data object NotEnabled : CapabilityStatus()
    data object Ready : CapabilityStatus()
}

enum class NoPermissionReason {
    NotRequested,
    DeniedPermanently,
    Restricted,
}
