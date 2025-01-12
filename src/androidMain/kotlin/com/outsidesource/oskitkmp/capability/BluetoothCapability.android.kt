package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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

internal class BluetoothKmpCapability(
    private val flags: Array<BluetoothCapabilityFlags>,
) : IInitializableKmpCapability, IKmpCapability {

    private var context: CapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var enableResultLauncher: ActivityResultLauncher<Intent>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val enableResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var bluetoothAdapter: BluetoothAdapter? = null

    private var hardwareSupportsCapability: Boolean = false
    private var hasRequestedPermissions: Boolean = false

    private val permissions = run {
        if (Build.VERSION.SDK_INT <= 30) return@run emptyList()

        flags.map {
            when (it) {
                BluetoothCapabilityFlags.Scan -> Manifest.permission.BLUETOOTH_SCAN
                BluetoothCapabilityFlags.Connect -> Manifest.permission.BLUETOOTH_CONNECT
                BluetoothCapabilityFlags.Advertise -> Manifest.permission.BLUETOOTH_ADVERTISE
            }
        }
    }.toTypedArray()

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = true
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

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
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

    override fun init(context: CapabilityContext) {
        this.context = context
        checkHardwareSupport()

        bluetoothAdapter = ContextCompat.getSystemService(context.activity, BluetoothManager::class.java)?.adapter

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

        if (bluetoothAdapter?.isEnabled == false) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    private fun checkHardwareSupport() {
        val activity = context?.activity ?: return
        val bluetoothClassicAvailable = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val bleAvailable = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        hardwareSupportsCapability = bleAvailable && bluetoothClassicAvailable
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
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            enableResultLauncher?.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            enableResultFlow.firstOrNull()
            return Outcome.Ok(getCurrentStatus())
        } catch (_: Throwable) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun openEnableSettingsScreen(): Outcome<Unit, Any> {
        try {
            val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            val openEnableSettings = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            activity.startActivity(openEnableSettings)
            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(context)
}
