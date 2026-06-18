package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import org.w3c.dom.mediacapture.MediaDevices
import kotlin.js.Promise

actual class KmpCapabilityContext()

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual fun createPlatformMicrophoneCapability(): IKmpCapability =
    MicrophoneKmpCapability()

internal actual suspend fun internalOpenAppSettingsScreen(
    context: KmpCapabilityContext?,
): Outcome<Unit, Any> = Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

internal external object navigator {
    object permissions {
        fun query(options: JsAny): Promise<PermissionStatus>
    }

    object geolocation {
        fun getCurrentPosition(success: (JsAny) -> Unit, error: (GeolocationPositionError) -> Unit)
    }

    object mediaDevices {
        fun getUserMedia(options: JsAny): Promise<JsAny>
    }
}

internal  external class PermissionStatus : JsAny {
    val name: String
    val state: String
    var onchange: () -> Unit
}

internal  external class GeolocationPositionError {
    val code: Int
    val message: String
}