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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class StorageKmpCapability(
    private val flags: Array<StorageCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {


    private var context: KmpCapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var hasRequestedPermissions: Boolean = false

    private val permissions = run {
        val result = mutableListOf<String>()

        flags.forEach { flag ->
            when (flag) {
                StorageCapabilityFlags.ReadExternal -> {
                    if (Build.VERSION.SDK_INT < 33) {
                        result += Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                }

                StorageCapabilityFlags.WriteExternal -> {
                    result += Manifest.permission.WRITE_EXTERNAL_STORAGE
                }

                StorageCapabilityFlags.ReadMedia -> {
                    if (Build.VERSION.SDK_INT >= 33) {
                        result += Manifest.permission.READ_MEDIA_AUDIO
                        result += Manifest.permission.READ_MEDIA_IMAGES
                        result += Manifest.permission.READ_MEDIA_VIDEO
                    }
                }

                StorageCapabilityFlags.WriteMedia -> Unit
            }
        }

        result
    }.toTypedArray()

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
        awaitClose {}
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context

        permissionResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                scope.launch { permissionsResultFlow.emit(Unit) }
            }
    }

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun queryStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown

        val hasAuthorization = permissions
            .all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }

        if (!hasAuthorization) {
            val reason = NoPermissionReason.NotRequested
            return CapabilityStatus.NoPermission(reason)
        }

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            withContext(Dispatchers.Main) {
                permissionResultLauncher?.launch(permissions)
            }
            permissionsResultFlow.firstOrNull()
            return Outcome.Ok(queryStatus())
        } catch (e: Exception) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> = Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(context)

}