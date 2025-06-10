package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
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

internal class NfcKmpCapability : IInitializableKmpCapability, IKmpCapability {

    private var context: KmpCapabilityContext? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val permissionsResultFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val permissions = if (Build.VERSION.SDK_INT < 33) {
        emptyArray()
    } else {
        arrayOf(Manifest.permission.NFC)
    }

    private var nfcAdapter: NfcAdapter? = null
    private var hardwareSupportsCapability = false
    private var hasRequestedPermissions = false

    override val hasPermissions: Boolean = permissions.isNotEmpty()
    override val hasEnablableService: Boolean = true
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = true

    override val status: Flow<CapabilityStatus> = callbackFlow {
        val activity = context?.activity ?: return@callbackFlow

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launch { send(queryStatus()) }
            }
        }
        ContextCompat.registerReceiver(activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        launch {
            activity.lifecycle.currentStateFlow.collect {
                when (it) {
                    Lifecycle.State.RESUMED -> send(queryStatus())
                    else -> {}
                }
            }
        }

        send(queryStatus())

        awaitClose {
            activity.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()

    override fun init(context: KmpCapabilityContext) {
        this.context = context

        val activity = context.activity
        hardwareSupportsCapability = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        if (permissions.isNotEmpty()) {
            permissionResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                scope.launch { permissionsResultFlow.emit(Unit) }
            }
        }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        val activity = context?.activity ?: return CapabilityStatus.Unknown
        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()

        val hasAuthorization = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasAuthorization) {
            val reason = if (hasRequestedPermissions) {
                NoPermissionReason.DeniedPermanently
            } else {
                NoPermissionReason.NotRequested
            }
            return CapabilityStatus.NoPermission(reason)
        }

        if (nfcAdapter?.isEnabled == false) return CapabilityStatus.NotEnabled

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        try {
            context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            withContext(Dispatchers.Main) {
                permissionResultLauncher?.launch(permissions)
            }
            permissionsResultFlow.firstOrNull()
            hasRequestedPermissions = true
            return Outcome.Ok(queryStatus())
        } catch (e: Exception) {
            return Outcome.Error(Unit)
        }
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> {
        return Outcome.Error(Unit)
    }

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> {
        try {
            val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            activity.startActivity(intent)
            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        internalOpenAppSettingsScreen(context)
}
