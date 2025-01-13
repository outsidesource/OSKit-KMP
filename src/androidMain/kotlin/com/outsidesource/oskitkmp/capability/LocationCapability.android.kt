package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
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

internal class LocationKmpCapability(
    private val flags: Array<LocationCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {

    private var context: KmpCapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var enableResultLauncher: ActivityResultLauncher<Intent>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val enableResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var locationManager: LocationManager? = null

    private var hardwareSupportsCapability: Boolean = false
    private var hasRequestedPermissions: Boolean = false

    private val permissions = flags
        .filter {
            if (Build.VERSION.SDK_INT < 29 && it == LocationCapabilityFlags.BackgroundLocation) return@filter false
            if (Build.VERSION.SDK_INT > 30 && it == LocationCapabilityFlags.BluetoothAccess) return@filter false
            true
        }
        .map {
            when (it) {
                LocationCapabilityFlags.BluetoothAccess -> Manifest.permission.ACCESS_FINE_LOCATION
                LocationCapabilityFlags.CoarseLocation -> Manifest.permission.ACCESS_COARSE_LOCATION
                LocationCapabilityFlags.FineLocation -> Manifest.permission.ACCESS_FINE_LOCATION
                LocationCapabilityFlags.BackgroundLocation -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }
        }.toTypedArray()

    private val isEnabledRequiredForFlags = when (flags.size) {
        1 -> if (flags[0] == LocationCapabilityFlags.BluetoothAccess) Build.VERSION.SDK_INT <= 30 else true
        else -> true
    }

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenEnableSettingsScreen: Boolean = true

    override val status: CapabilityStatus
        get() = getCurrentStatus()

    override val statusFlow: Flow<CapabilityStatus> = callbackFlow {
        val activity = context?.activity ?: return@callbackFlow

        launch {
            activity.lifecycle.currentStateFlow.collect {
                when (it) {
                    Lifecycle.State.RESUMED -> send(getCurrentStatus())
                    else -> {}
                }
            }
        }

        val filter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launch { send(getCurrentStatus()) }
            }
        }
        ContextCompat.registerReceiver(activity, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        send(getCurrentStatus())

        awaitClose {
            activity.unregisterReceiver(broadcastReceiver)
        }
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context
        checkHardwareSupport()

        locationManager = ContextCompat.getSystemService(context.activity, LocationManager::class.java)

        permissionResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                scope.launch { permissionsResultFlow.emit(Unit) }
            }

        enableResultLauncher = context.activity
            .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                scope.launch { enableResultFlow.emit(Unit) }
            }
    }

    private fun getCurrentStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown

        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()

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

        if (isEnabledRequiredForFlags && locationManager?.isLocationEnabled == false) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    private fun checkHardwareSupport() {
        val activity = context?.activity ?: return
        hardwareSupportsCapability = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            withContext(Dispatchers.Main) { permissionResultLauncher?.launch(permissions) }
            permissionsResultFlow.firstOrNull()
            hasRequestedPermissions = true
            return Outcome.Ok(getCurrentStatus())
        } catch (e: Exception) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        return Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
    }

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> {
        try {
            val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            val openEnableSettings = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(openEnableSettings)
            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(context)
}
