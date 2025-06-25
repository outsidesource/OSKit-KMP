package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationPermission
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class LocalNotificationsKmpCapability : IInitializableKmpCapability, IKmpCapability {

    private val localStatusFlow = MutableStateFlow<CapabilityStatus>(
        if (!hardwareSupportsCapability()) {
            CapabilityStatus.Unsupported()
        } else {
            CapabilityStatus.Unknown
        },
    )

    override val status: Flow<CapabilityStatus> = localStatusFlow

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override fun init(context: KmpCapabilityContext) {
        scope.launch {
            if (!hardwareSupportsCapability()) return@launch

            val permission = Notification.permission
            localStatusFlow.value = mapPermissionToCapabilityStatus(permission)
        }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        if (!hardwareSupportsCapability()) return CapabilityStatus.Unsupported()
        return mapPermissionToCapabilityStatus(Notification.permission)
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        if (!hardwareSupportsCapability()) return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

        return suspendCoroutine { continuation ->
            Notification.requestPermission { permission ->
                val status = mapPermissionToCapabilityStatus(permission)
                localStatusFlow.value = status
                continuation.resume(Outcome.Ok(status))
            }
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    private fun mapPermissionToCapabilityStatus(permission: NotificationPermission): CapabilityStatus {
        return when (permission.toString()) {
            "granted" -> CapabilityStatus.Ready
            "default" -> CapabilityStatus.NoPermission(NoPermissionReason.NotRequested)
            "denied" -> CapabilityStatus.NoPermission(NoPermissionReason.DeniedPermanently)
            else -> CapabilityStatus.Unknown
        }
    }
}

private fun hardwareSupportsCapability(): Boolean = js("""typeof Notification !== undefined""")
