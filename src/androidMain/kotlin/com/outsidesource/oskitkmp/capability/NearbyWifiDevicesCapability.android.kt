package com.outsidesource.oskitkmp.capability

import android.Manifest
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class NearbyWifiDevicesKmpCapability() : IInitializableKmpCapability, IKmpCapability {

    private var context: KmpCapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var hasRequestedPermissions: Boolean = false
    private val permissions = run {
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            emptyArray()
        }
    }

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override val status: Flow<CapabilityStatus> = callbackFlow {
        val activity = context?.activity ?: return@callbackFlow

        launch {
            activity.lifecycle.currentStateFlow.collect {
                when (it) {
                    Lifecycle.State.RESUMED -> send(queryStatus())
                    else -> {}
                }
            }
        }

        send(queryStatus())

        awaitClose { }
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context

        permissionResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                scope.launch { permissionsResultFlow.emit(Unit) }
            }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown

        if (permissions.isEmpty()) {
            return CapabilityStatus.Unsupported()
        }

        val hasAuthorization = permissions
            .all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }

        if (!hasAuthorization) {
            val reason = if (hasRequestedPermissions) {
                NoPermissionReason.DeniedPermanently
            } else {
                NoPermissionReason.NotRequested
            }
            return CapabilityStatus.NoPermission(reason)
        }

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            withContext(Dispatchers.Main) { permissionResultLauncher?.launch(permissions) }
            permissionsResultFlow.firstOrNull()
            hasRequestedPermissions = true
            return Outcome.Ok(queryStatus())
        } catch (_: Exception) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
    }

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> {
        return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        internalOpenAppSettingsScreen(context)
}
