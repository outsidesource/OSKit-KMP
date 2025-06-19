package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class LocalNotificationsKmpCapability : IInitializableKmpCapability, IKmpCapability {

    private var context: KmpCapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var permissionResultLauncher: ActivityResultLauncher<String>? = null
    private val permissionResultFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var hasRequestedPermissions: Boolean = false

    private val permission: String? = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    override val hasPermissions: Boolean = permission != null
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override val status: Flow<CapabilityStatus> = callbackFlow {
        val activity = context?.activity ?: return@callbackFlow

        launch {
            activity.lifecycle.currentStateFlow.collect {
                if (it == Lifecycle.State.RESUMED) {
                    send(queryStatus())
                }
            }
        }

        send(queryStatus())

        awaitClose { }
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context

        permissionResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                scope.launch {
                    permissionResultFlow.emit(Unit)
                }
            }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown

        if (permission != null) {
            val granted = ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED

            if (!granted) {
                val reason = if (hasRequestedPermissions) {
                    NoPermissionReason.DeniedPermanently
                } else {
                    NoPermissionReason.NotRequested
                }
                return CapabilityStatus.NoPermission(reason)
            }
        }

        val manager = ContextCompat.getSystemService(activity, NotificationManager::class.java)
        if (manager?.areNotificationsEnabled() != true) {
            return CapabilityStatus.NotEnabled
        }

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)

            if (permission != null) {
                withContext(Dispatchers.Main) {
                    permissionResultLauncher?.launch(permission)
                }
                permissionResultFlow.firstOrNull()
                hasRequestedPermissions = true
            }

            return Outcome.Ok(queryStatus())
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
    }

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> {
        return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> {
        return internalOpenAppSettingsScreen(context)
    }
}
